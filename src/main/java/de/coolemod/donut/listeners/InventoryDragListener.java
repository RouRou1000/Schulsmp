package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Blockiert Drag-and-Drop in allen GUIs
 */
public class InventoryDragListener implements Listener {
    private final DonutPlugin plugin;

    public InventoryDragListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        
        String title = e.getView().getTitle();
        
        // Blockiere Drag in ALLEN GUIs außer normalen Chests
        if (title.contains("Shop") || 
            title.contains("ᴀᴜᴋᴛɪᴏɴѕʜᴀᴜѕ") || title.contains("AUKTIONSHAUS") ||
            title.contains("ᴍᴇɪɴᴇ ᴀᴜᴋᴛɪᴏɴᴇɴ") || title.contains("MEINE AUKTIONEN") ||
            title.contains("Orders") || 
            title.contains("Kiste") || 
            title.contains("Öffne:") ||
            title.contains("DONUT CORE")) {
            e.setCancelled(true);
            return;
        }
        
        // Spezielle Logik für Sell und AuctionCreate GUIs
        if (title.contains("ɪᴛᴇᴍѕ ᴠᴇʀᴋᴀᴜꜰᴇɴ") || title.contains("ITEMS VERKAUFEN") ||
            title.contains("ᴀᴜᴋᴛɪᴏɴ ᴇʀѕᴛᴇʟʟᴇɴ") || title.contains("AUKTION ERSTELLEN")) {
            
            // Prüfe ob Drag in erlaubte Slots geht
            for (int slot : e.getRawSlots()) {
                // Top Inventory Slots
                if (slot < 54) {
                    // Erlaube nur Slots 10-43 (ohne Borders)
                    if (slot < 10 || slot >= 44 || slot % 9 == 0 || slot % 9 == 8) {
                        e.setCancelled(true);
                        return;
                    }
                    
                    // Blockiere Drag auf GUI-Items
                    ItemStack item = e.getView().getTopInventory().getItem(slot);
                    if (item != null && item.hasItemMeta()) {
                        if (item.getItemMeta().getPersistentDataContainer().has(
                            new org.bukkit.NamespacedKey(plugin, "donut_gui_action"),
                            org.bukkit.persistence.PersistentDataType.STRING)) {
                            e.setCancelled(true);
                            return;
                        }
                        if (item.getType() == Material.BLACK_STAINED_GLASS_PANE || 
                            item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                            e.setCancelled(true);
                            return;
                        }
                    }
                }
            }
        }
    }
}
