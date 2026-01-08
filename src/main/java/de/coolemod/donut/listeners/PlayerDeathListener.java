package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Aktualisiert Kills/Deaths und gibt Shards bei PvP-Kill
 */
public class PlayerDeathListener implements Listener {
    private final DonutPlugin plugin;

    public PlayerDeathListener(DonutPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        plugin.getStats().addDeath(victim.getUniqueId());
        if (killer != null) {
            plugin.getStats().addKill(killer.getUniqueId());
            int shards = plugin.getConfig().getInt("settings.shard-per-kill", 10);
            plugin.getShards().addShards(killer.getUniqueId(), shards);
            killer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§aDu hast " + shards + " Shards für den Kill erhalten.");
        }
    }
}