package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import de.coolemod.donut.managers.CrateManager;

/**
 * Öffnet Kisten (physische Kisten), die als "Tier X Kiste" benannt sind.
 */
public class PlayerInteractListener implements Listener {
    private final DonutPlugin plugin;

    public PlayerInteractListener(DonutPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (b.getType() == Material.CHEST && b.getState() instanceof Chest) {
            Chest chest = (Chest) b.getState();
            if (chest.getCustomName() != null) {
                e.setCancelled(true);
                String name = chest.getCustomName();
                // Versuche, eine Crate mit dieser ID oder diesem Display-Namen zu finden
                String found = null;
                for (String id : plugin.getCrateManager().getCrateIds()) {
                    CrateManager.Crate c = plugin.getCrateManager().getCrate(id);
                    if (c.id.equalsIgnoreCase(name) || c.display.equalsIgnoreCase(name)) { found = id; break; }
                }
                if (found != null) {
                    plugin.getCrateManager().openCrateAnimated(e.getPlayer(), found);
                }
            }
        }
    }
}
