package de.coolemod.schulcore.listeners;

import de.coolemod.schulcore.SchulCorePlugin;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * VERALTET - Das neue Spawner-System in de.coolemod.schulcore.spawner.SpawnerListener
 * handhabt jetzt alle Spawner. Dieser Listener ist nur noch für Vanilla-Spawner.
 */
public class SpawnerBreakListener implements Listener {
    private final SchulCorePlugin plugin;

    public SpawnerBreakListener(SchulCorePlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent e) {
        // Ignoriere wenn neues SpawnerSystem den Spawner kennt
        if (e.getBlock().getType() == Material.SPAWNER) {
            if (plugin.getSpawnerSystem() != null && 
                plugin.getSpawnerSystem().getPlacedSpawner(e.getBlock().getLocation()) != null) {
                // Neues System handhabt diesen Spawner
                return;
            }
        }
        // Nur für Vanilla-Spawner (nicht vom neuen System)
        plugin.getSpawnerManager().handleBlockBreak(e);
    }
}
