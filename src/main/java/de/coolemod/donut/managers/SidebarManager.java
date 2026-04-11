package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

/**
 * Robustere Sidebar: läuft synchron (vermeidet Race-Conditions) und erlaubt direkte Anwendung beim Join.
 */
public class SidebarManager {
    private final DonutPlugin plugin;
    private BukkitTask task;
    private Object luckPerms;

    public SidebarManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.luckPerms = resolveLuckPerms();
    }

    public void start() {
        if (task != null) task.cancel();
        int ticks = Math.max(1, plugin.getConfig().getInt("settings.scoreboard-update-ticks", 20));
        // WICHTIG: Scoreboards sollten auf dem Hauptthread erstellt/gesetzt werden
        try {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, ticks);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Scoreboard-Timer konnte nicht gestartet werden: " + throwable.getMessage());
            task = null;
        }
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }

    /**
     * Sofort das Scoreboard für einen Spieler anwenden.
     */
    public void applyFor(Player p) {
        updateFor(p);
    }

    private void updateAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            updateFor(p);
        }
    }

    private void updateFor(Player p) {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        Scoreboard board = sm.getNewScoreboard();
        Objective obj = board.registerNewObjective("donut_sidebar", "dummy", "§6⚡ §eѕᴄʜᴜʟ-ѕᴍᴘ §6⚡");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Rang aus LuckPerms
        String rankDisplay = getRankDisplay(p);

        obj.getScore("§8§m━━━━━━━━━━━━━━").setScore(12);

        String name = "§b▸ §f" + p.getName();
        obj.getScore(name).setScore(11);

        String clan = "§d✦ §7ᴄʟᴀɴ§8: " + plugin.getClanManager().getSidebarClanName(p.getUniqueId());
        obj.getScore(clan).setScore(10);

        obj.getScore("§8  " + rankDisplay).setScore(9);

        obj.getScore(" ").setScore(8);

        String money = "§6⛃ §7ɡᴇʟᴅ§8: §a" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(p.getUniqueId()));
        obj.getScore(money).setScore(7);

        String shards = "§b❖ §7ѕʜᴀʀᴅѕ§8: §b" + NumberFormatter.formatInt(plugin.getShards().getShards(p.getUniqueId()));
        obj.getScore(shards).setScore(6);

        obj.getScore("  ").setScore(5);

        String kills = "§c⚔ §7ᴋɪʟʟѕ§8: §e" + plugin.getStats().getKills(p.getUniqueId());
        obj.getScore(kills).setScore(4);

        String deaths = "§4☠ §7ᴅᴇᴀᴛʜѕ§8: §c" + plugin.getStats().getDeaths(p.getUniqueId());
        obj.getScore(deaths).setScore(3);

        String kd = "§6★ §7ᴋ/ᴅ§8: §f" + "%.2f".formatted(plugin.getStats().getKD(p.getUniqueId()));
        obj.getScore(kd).setScore(2);

        obj.getScore("   ").setScore(1);
        obj.getScore("§7ᴘʟᴀʏ.ѕᴄʜᴜʟ-ѕᴍᴘ.ᴅᴇ").setScore(0);

        p.setScoreboard(board);
    }

    /**
     * Holt den Rang-Display aus LuckPerms.
     */
    public String getRankDisplay(Player p) {
        Object user = getLuckPermsUser(p);
        if (user == null) return "§7Spieler";

        String prefix = getLuckPermsMetaValue(user, "getPrefix");
        if (prefix != null && !prefix.isEmpty()) return prefix;

        String group = getPrimaryGroup(user);
        if (group != null && !group.isEmpty()) {
            return "§7" + group.substring(0, 1).toUpperCase() + group.substring(1);
        }
        return "§7Spieler";
    }

    /**
     * Holt den Tab-Display-Namen: [Prefix] Spielername
     */
    public String getTabName(Player p) {
        Object user = getLuckPermsUser(p);
        if (user == null) return "§7" + p.getName();

        String prefix = getLuckPermsMetaValue(user, "getPrefix");
        String suffix = getLuckPermsMetaValue(user, "getSuffix");
        String nameColor = suffix != null && !suffix.isEmpty() ? suffix : "§7";
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + " §8┃ " + nameColor + p.getName();
        }
        return "§7" + p.getName();
    }

    /**
     * Holt den Chat-Prefix für Chat-Formatting.
     */
    public String getChatFormat(Player p) {
        return getTabName(p);
    }

    private Object resolveLuckPerms() {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            return null;
        }

        try {
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            return providerClass.getMethod("get").invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object getLuckPermsUser(Player player) {
        if (luckPerms == null) {
            luckPerms = resolveLuckPerms();
            if (luckPerms == null) {
                return null;
            }
        }

        try {
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            return userManager.getClass().getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUniqueId());
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String getLuckPermsMetaValue(Object user, String methodName) {
        try {
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            Object value = metaData.getClass().getMethod(methodName).invoke(metaData);
            return value instanceof String stringValue ? stringValue : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String getPrimaryGroup(Object user) {
        try {
            Object value = user.getClass().getMethod("getPrimaryGroup").invoke(user);
            return value instanceof String stringValue ? stringValue : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
