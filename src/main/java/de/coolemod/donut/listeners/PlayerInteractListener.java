package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.CrateDetailGUI;
import de.coolemod.donut.managers.CrateManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Öffnet Kisten (physische Kisten + Rechtsklick auf Key → Detail-GUI).
 */
public class PlayerInteractListener implements Listener {
    private final DonutPlugin plugin;

    public PlayerInteractListener(DonutPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        // Rechtsklick mit Key → Detail-GUI der Kiste öffnen
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack hand = e.getItem();
            if (hand != null && hand.getType() == Material.TRIPWIRE_HOOK && hand.hasItemMeta()) {
                ItemMeta meta = hand.getItemMeta();
                NamespacedKey key = new NamespacedKey(plugin, "donut_crate_id");
                if (meta.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    String crateId = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
                    if (plugin.getCrateManager().hasCrate(crateId)) {
                        e.setCancelled(true);
                        new CrateDetailGUI(plugin).open(e.getPlayer(), crateId);
                        return;
                    }
                }
            }
        }

        // Physische Kisten
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (b.getType() == Material.CHEST && b.getState() instanceof Chest) {
            Chest chest = (Chest) b.getState();
            if (chest.getCustomName() != null) {
                e.setCancelled(true);
                String name = chest.getCustomName();
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
