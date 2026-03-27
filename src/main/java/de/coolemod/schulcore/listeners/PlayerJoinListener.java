package de.coolemod.schulcore.listeners;

import de.coolemod.schulcore.SchulCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Initialisiert Scoreboard bei Join + Spawn für neue Spieler
 */
public class PlayerJoinListener implements Listener {
    private final SchulCorePlugin plugin;

    public PlayerJoinListener(SchulCorePlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Rang-Permissions und Tab-Name
        if (plugin.getRankManager() != null) {
            plugin.getRankManager().applyPermissions(e.getPlayer());
            plugin.getRankManager().applyTabName(e.getPlayer());
        }

        // Scoreboard
        plugin.getScoreboardManager().applyFor(e.getPlayer());

        // Neue Spieler zum World-Spawnpoint teleportieren + Info
        if (!e.getPlayer().hasPlayedBefore()) {
            Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation().add(0.5, 0, 0.5);
            e.getPlayer().teleport(spawn);

            e.getPlayer().sendMessage("");
            e.getPlayer().sendMessage("§8§m                                                  ");
            e.getPlayer().sendMessage("§6§l  ⭐ Willkommen auf dem §e§lSchul-SMP§6§l! ⭐");
            e.getPlayer().sendMessage("");
            e.getPlayer().sendMessage("§7  Der §c§lNether §7ist §a§l1:1 §7(keine 1:8 Ratio)!");
            e.getPlayer().sendMessage("§7  §81 Block Nether = 1 Block Overworld");
            e.getPlayer().sendMessage("");
            e.getPlayer().sendMessage("§7  §e§l➤ §7Geh in §b§lA-16 §7für das §a§lTutorial§7!");
            e.getPlayer().sendMessage("");
            e.getPlayer().sendMessage("§8§m                                                  ");
            e.getPlayer().sendMessage("");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (plugin.getRankManager() != null) {
            plugin.getRankManager().removeAttachment(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        // Kein Bett/Anker → zum Hauptwelt-Spawn
        if (!e.isBedSpawn() && !e.isAnchorSpawn()) {
            Location spawn = Bukkit.getWorlds().get(0).getSpawnLocation().add(0.5, 0, 0.5);
            e.setRespawnLocation(spawn);
        }
    }
}
