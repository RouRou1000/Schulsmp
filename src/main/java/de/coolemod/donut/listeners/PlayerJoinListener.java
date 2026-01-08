package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Initialisiert Scoreboard bei Join
 */
public class PlayerJoinListener implements Listener {
    private final DonutPlugin plugin;

    public PlayerJoinListener(DonutPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        // Nur Scoreboard für den betreffenden Spieler sofort anwenden
        plugin.getScoreboardManager().applyFor(e.getPlayer());
    }
}
