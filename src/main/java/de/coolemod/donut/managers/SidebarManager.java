package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
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

    public SidebarManager(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (task != null) task.cancel();
        int ticks = plugin.getConfig().getInt("settings.scoreboard-update-ticks", 20);
        // WICHTIG: Scoreboards sollten auf dem Hauptthread erstellt/gesetzt werden
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 0L, ticks);
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
        Objective obj = board.registerNewObjective("donut_sidebar", "dummy", "§6§l⚡ §e§lѕᴄʜᴜʟ-ѕᴍᴘ §6§l⚡");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("§8§m━━━━━━━━━━━━━━").setScore(11);
        
        String name = "§b▸ §f" + p.getName();
        obj.getScore(name).setScore(10);
        
        obj.getScore(" ").setScore(9);
        
        String money = "§6⛃ §7ɢᴇʟᴅ§8: §a§l$" + String.format("%.2f", plugin.getEconomy().getBalance(p.getUniqueId()));
        obj.getScore(money).setScore(8);
        
        String shards = "§b❖ §7ѕʜᴀʀᴅѕ§8: §b§l" + plugin.getShards().getShards(p.getUniqueId());
        obj.getScore(shards).setScore(7);
        
        obj.getScore("  ").setScore(6);
        
        String kills = "§c⚔ §7ᴋɪʟʟѕ§8: §e§l" + plugin.getStats().getKills(p.getUniqueId());
        obj.getScore(kills).setScore(5);
        
        String deaths = "§4☠ §7ᴅᴇᴀᴛʜѕ§8: §c§l" + plugin.getStats().getDeaths(p.getUniqueId());
        obj.getScore(deaths).setScore(4);
        
        String kd = "§6★ §7ᴋ/ᴅ§8: §f§l" + String.format("%.2f", plugin.getStats().getKD(p.getUniqueId()));
        obj.getScore(kd).setScore(3);
        
        obj.getScore("   ").setScore(2);
        obj.getScore("§8§m━━━━━━━━━━━━━━").setScore(1);
        obj.getScore("§7ᴘʟᴀʏ.ѕᴄʜᴜʟ-ѕᴍᴘ.ᴅᴇ").setScore(0);

        p.setScoreboard(board);
    }
}
