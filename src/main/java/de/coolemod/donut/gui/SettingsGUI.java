package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.SettingsManager;
import de.coolemod.donut.managers.SettingsManager.Setting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * /settings GUI - Spieler können Einstellungen ein/ausschalten
 */
public class SettingsGUI {
    private final DonutPlugin plugin;

    private static final String TITLE = "§8§l⚙ §d§lEINSTELLUNGEN";

    public SettingsGUI(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    public static boolean isSettingsGUI(String title) {
        return title != null && title.contains("EINSTELLUNGEN");
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE);
        UUID uuid = player.getUniqueId();
        SettingsManager sm = plugin.getSettingsManager();

        // Rahmen füllen
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName("§r");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 36; i++) inv.setItem(i, filler);

        // Settings-Slots (mittige Reihe: 10-16, untere: 19-25)
        placeToggle(inv, 10, sm, uuid, Setting.TPA_ENABLED,
                Material.ENDER_PEARL, "§b§lTPA Anfragen",
                "§7Erlaube anderen Spielern dir", "§7TPA-Anfragen zu senden.");

        placeToggle(inv, 11, sm, uuid, Setting.TPAHERE_ENABLED,
                Material.ENDER_EYE, "§b§lTPAHere Anfragen",
                "§7Erlaube anderen Spielern dir", "§7TPAHere-Anfragen zu senden.");

        placeToggle(inv, 13, sm, uuid, Setting.MOB_SPAWN_BLOCK,
                Material.ZOMBIE_HEAD, "§c§lMob-Schutz",
                "§7Verhindert dass feindliche", "§7Mobs in deiner Nähe spawnen.",
                "§8(16 Block Radius)");

        placeToggle(inv, 14, sm, uuid, Setting.CLAN_GLOW,
                Material.GLOWSTONE_DUST, "§e§lClan Glow",
                "§7Deine Clan-Mitglieder leuchten", "§7für dich mit einem Glow-Effekt.");

        placeToggle(inv, 19, sm, uuid, Setting.GLOBAL_CHAT,
                Material.WRITABLE_BOOK, "§f§lGlobaler Chat",
                "§7Zeige globale Chat-Nachrichten", "§7von anderen Spielern an.");

        placeToggle(inv, 20, sm, uuid, Setting.DEATH_MESSAGES,
                Material.SKELETON_SKULL, "§7§lTodesnachrichten",
                "§7Zeige Todesnachrichten", "§7anderer Spieler an.");

        placeToggle(inv, 22, sm, uuid, Setting.SCOREBOARD,
                Material.PAINTING, "§6§lScoreboard",
                "§7Zeige das Scoreboard", "§7auf der rechten Seite an.");

        player.openInventory(inv);
    }

    private void placeToggle(Inventory inv, int slot, SettingsManager sm, UUID uuid,
                              Setting setting, Material icon, String name, String... descLines) {
        boolean enabled = sm.getSetting(uuid, setting);
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add("§8");
        for (String line : descLines) lore.add(line);
        lore.add("§8");
        lore.add(enabled ? "§a§l✔ Aktiviert" : "§c§l✖ Deaktiviert");
        lore.add("§8");
        lore.add("§e▸ Klicken zum Umschalten");
        meta.setLore(lore);

        // Store setting key in PDC
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "setting_key");
        meta.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.STRING, setting.name());
        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    public void handleClick(Player player, ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return;
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(plugin, "setting_key");
        String settingName = clicked.getItemMeta().getPersistentDataContainer()
                .get(key, org.bukkit.persistence.PersistentDataType.STRING);
        if (settingName == null) return;

        Setting setting;
        try {
            setting = Setting.valueOf(settingName);
        } catch (IllegalArgumentException e) {
            return;
        }

        SettingsManager sm = plugin.getSettingsManager();
        sm.toggleSetting(player.getUniqueId(), setting);
        boolean newValue = sm.getSetting(player.getUniqueId(), setting);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, newValue ? 1.2f : 0.8f);
        player.sendMessage("§8┃ §d§lSETTINGS §8┃ §f" + setting.getDisplayName() + " §7ist jetzt "
                + (newValue ? "§a§lAN" : "§c§lAUS") + "§7.");

        // Sofort GUI aktualisieren
        open(player);

        // Scoreboard sofort anwenden
        if (setting == Setting.SCOREBOARD) {
            if (newValue) {
                if (plugin.getScoreboardManager() != null) {
                    plugin.getScoreboardManager().applyFor(player);
                }
            } else {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }

        // Clan Glow sofort anwenden
        if (setting == Setting.CLAN_GLOW) {
            applyClanGlow(player, newValue);
        }
    }

    private void applyClanGlow(Player player, boolean enabled) {
        var clanMgr = plugin.getClanManager();
        if (clanMgr == null) return;
        var clan = clanMgr.getClan(player.getUniqueId());
        if (clan == null) return;

        for (UUID memberId : clan.getMembers()) {
            if (memberId.equals(player.getUniqueId())) continue;
            Player member = Bukkit.getPlayer(memberId);
            if (member == null) continue;

            if (enabled) {
                // Glow via Scoreboard-Team für nur diesen Spieler
                org.bukkit.scoreboard.Scoreboard sb = player.getScoreboard();
                String teamName = "cglow_" + member.getName().substring(0, Math.min(member.getName().length(), 10));
                org.bukkit.scoreboard.Team team = sb.getTeam(teamName);
                if (team == null) team = sb.registerNewTeam(teamName);
                team.setColor(org.bukkit.ChatColor.LIGHT_PURPLE);
                team.addEntry(member.getName());
                member.setGlowing(true);
            } else {
                // Glow entfernen
                org.bukkit.scoreboard.Scoreboard sb = player.getScoreboard();
                String teamName = "cglow_" + member.getName().substring(0, Math.min(member.getName().length(), 10));
                org.bukkit.scoreboard.Team team = sb.getTeam(teamName);
                if (team != null) {
                    team.removeEntry(member.getName());
                    team.unregister();
                }
                // Nur wenn kein anderer Spieler Glow aktiv hat
                boolean anyoneElseGlows = false;
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (online.equals(player)) continue;
                    if (plugin.getSettingsManager().getSetting(online.getUniqueId(), Setting.CLAN_GLOW)) {
                        var otherClan = clanMgr.getClan(online.getUniqueId());
                        if (otherClan != null && otherClan.getMembers().contains(member.getUniqueId())) {
                            anyoneElseGlows = true;
                            break;
                        }
                    }
                }
                if (!anyoneElseGlows) member.setGlowing(false);
            }
        }
    }
}
