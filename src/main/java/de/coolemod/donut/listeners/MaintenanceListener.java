package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class MaintenanceListener implements Listener {
    private final DonutPlugin plugin;

    public MaintenanceListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (!plugin.getMaintenanceManager().isEnabled()) {
            return;
        }
        if (plugin.getMaintenanceManager().canBypass(event.getPlayer())) {
            return;
        }
        event.disallow(PlayerLoginEvent.Result.KICK_OTHER, plugin.getMaintenanceManager().getKickMessage());
    }
}