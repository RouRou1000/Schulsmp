package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.SettingsManager;
import de.coolemod.donut.managers.SettingsManager.Setting;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Listener für Settings-Features:
 * - Mob-Spawn-Schutz (blockiert Mob-Spawns in der Nähe)
 * - Todesnachrichten filtern
 */
public class SettingsListener implements Listener {
    private final DonutPlugin plugin;
    private static final int MOB_BLOCK_RADIUS = 16;

    public SettingsListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (!(e.getEntity() instanceof Monster)) return;
        // Nur natürliche Spawns blocken
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;

        SettingsManager sm = plugin.getSettingsManager();
        if (sm == null) return;

        for (Player nearby : e.getEntity().getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(e.getLocation()) <= MOB_BLOCK_RADIUS * MOB_BLOCK_RADIUS) {
                if (sm.getSetting(nearby.getUniqueId(), Setting.MOB_SPAWN_BLOCK)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (e.getDeathMessage() == null) return;
        SettingsManager sm = plugin.getSettingsManager();
        if (sm == null) return;

        // Todesnachricht nur an Spieler senden die sie sehen wollen
        String deathMessage = e.getDeathMessage();
        e.setDeathMessage(null); // Globale Nachricht unterdrücken

        for (Player online : e.getEntity().getServer().getOnlinePlayers()) {
            if (sm.getSetting(online.getUniqueId(), Setting.DEATH_MESSAGES)) {
                online.sendMessage(deathMessage);
            }
        }
    }
}
