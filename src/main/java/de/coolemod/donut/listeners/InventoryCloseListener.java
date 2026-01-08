package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Verhindert Item-Verlust beim Schließen von GUIs
 */
public class InventoryCloseListener implements Listener {
    private final DonutPlugin plugin;

    public InventoryCloseListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        Player p = (Player) e.getPlayer();
        Inventory inv = e.getInventory();

        // Bei Auction Create GUI - Items zurückgeben
        if (title.contains("ᴀᴜᴋᴛɪᴏɴ ᴇʀѕᴛᴇʟʟᴇɴ") || title.contains("AUKTION ERSTELLEN")) {
            // Nicht zurückgeben wenn Spieler gerade Preis eingibt
            if (!p.hasMetadata("ah_setting_price")) {
                returnItemsToPlayer(p, inv, 10, 43);
                // Lösche gespeicherten Preis und Items aus HashMap
                de.coolemod.donut.gui.AuctionCreateGUI.clearPrice(p);
                de.coolemod.donut.gui.AuctionCreateGUI.clearItems(p);
            }
        }
        
        // Bei Sell GUI - Items zurückgeben
        if (title.contains("ɪᴛᴇᴍѕ ᴠᴇʀᴋᴀᴜꜰᴇɴ") || title.contains("ITEMS VERKAUFEN")) {
            returnItemsToPlayer(p, inv, 10, 43);
        }
    }

    /**
     * Gibt Items aus dem angegebenen Slot-Bereich an den Spieler zurück
     */
    private void returnItemsToPlayer(Player p, Inventory inv, int startSlot, int endSlot) {
        for (int i = startSlot; i <= endSlot; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                // Überspringe GUI-Items (Border, Buttons, etc.)
                if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(
                        new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), 
                        org.bukkit.persistence.PersistentDataType.STRING)) {
                    continue;
                }
                
                // Überspringe Border-Items
                if (item.getType() == Material.BLACK_STAINED_GLASS_PANE || item.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                    continue;
                }
                
                // Versuche Item ins Inventar zu geben
                if (p.getInventory().firstEmpty() != -1) {
                    p.getInventory().addItem(item);
                } else {
                    // Wenn voll, dann droppe es
                    p.getWorld().dropItemNaturally(p.getLocation(), item);
                }
            }
        }
    }
}
