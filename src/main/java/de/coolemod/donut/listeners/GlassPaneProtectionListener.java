package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

/**
 * ZUSÄTZLICHER Schutz gegen Glass Panes - läuft als LETZTES
 */
public class GlassPaneProtectionListener implements Listener {
    private final DonutPlugin plugin;

    public GlassPaneProtectionListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onInventoryClickMonitor(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        
        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor = e.getCursor();
        
        // Prüfe geklicktes Item
        if (clicked != null && isGlassPane(clicked)) {
            if (e.getRawSlot() < e.getView().getTopInventory().getSize()) {
                if (!hasActionKey(clicked)) {
                    e.setCancelled(true);
                    e.setCursor(null);
                }
            }
        }
        
        // Prüfe Cursor
        if (cursor != null && isGlassPane(cursor)) {
            if (!hasActionKey(cursor)) {
                e.setCancelled(true);
                e.setCursor(null);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player p = (Player) e.getPlayer();
        
        // Entferne Glass Panes ohne PDC aus dem Inventar
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (int i = 0; i < p.getInventory().getSize(); i++) {
                ItemStack item = p.getInventory().getItem(i);
                if (item != null && isGlassPane(item) && !hasActionKey(item)) {
                    p.getInventory().setItem(i, null);
                    p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ GUI-Items können nicht behalten werden!");
                }
            }
        }, 1L);
    }
    
    private boolean isGlassPane(ItemStack item) {
        return item.getType().name().contains("STAINED_GLASS_PANE");
    }
    
    private boolean hasActionKey(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        org.bukkit.NamespacedKey key1 = new org.bukkit.NamespacedKey(plugin, "donut_gui_action");
        org.bukkit.NamespacedKey key2 = new org.bukkit.NamespacedKey(plugin, "ah_action");
        
        return meta.getPersistentDataContainer().has(key1, org.bukkit.persistence.PersistentDataType.STRING) ||
               meta.getPersistentDataContainer().has(key2, org.bukkit.persistence.PersistentDataType.STRING);
    }
}
