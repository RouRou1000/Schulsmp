package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.ShopGUI_NEW;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * NEUER Shop Listener - nutzt InventoryHolder für maximale Sicherheit
 */
public class ShopListener_NEW implements Listener {
    private final DonutPlugin plugin;
    
    public ShopListener_NEW(DonutPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof ShopGUI_NEW)) return;
        
        Player p = (Player) e.getWhoClicked();
        
        // KRITISCH: Blockiere ALLES
        e.setCancelled(true);
        
        // Blockiere Klicks im eigenen Inventar
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) {
            return;
        }
        
        // Nur LEFT-Click erlauben
        if (e.getClick() != org.bukkit.event.inventory.ClickType.LEFT) {
            return;
        }
        
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        NamespacedKey actionKey = new NamespacedKey(plugin, "shop_action");
        if (!clicked.getItemMeta().getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            // Kein Action-Key = Shop-Item zum Kaufen
            handlePurchase(p, e.getView().getTitle(), e.getSlot());
            return;
        }
        
        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        ShopGUI_NEW shop = new ShopGUI_NEW(plugin);
        
        switch (action) {
            case "category_food":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openFoodShop(p);
                break;
            case "category_gear":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openGearShop(p);
                break;
            case "category_nether":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openNetherShop(p);
                break;
            case "category_shards":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openShardShop(p);
                break;
            case "category_end":
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                p.sendMessage("§8┃ §5§lEND SHOP §8┃ §cNoch nicht verfügbar!");
                break;
            case "back":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openMainMenu(p);
                break;
            case "close":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                p.closeInventory();
                break;
        }
    }
    
    private void handlePurchase(Player p, String title, int slot) {
        ShopGUI_NEW.ShopItem item = ShopGUI_NEW.getShopItem(title, slot);
        if (item == null) return;
        
        if (item.isShard) {
            // Shard-Kauf
            int shards = plugin.getShards().getShards(p.getUniqueId());
            if (shards < item.price) {
                p.sendMessage("");
                p.sendMessage("§8┃ §c§l✖ SHARD SHOP §8┃ §cNicht genug Shards!");
                p.sendMessage("§8┃ §7Benötigt§8: §d" + item.price + " Shards");
                p.sendMessage("§8┃ §7Deine Shards§8: §d" + shards);
                p.sendMessage("§8┃ §7Fehlt§8: §c" + (item.price - shards) + " Shards");
                p.sendMessage("");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            
            if (plugin.getShards().removeShards(p.getUniqueId(), item.price)) {
                // Gib Spawner
                if (item.spawnerType != null && item.material == Material.SPAWNER) {
                    try {
                        org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(item.spawnerType);
                        ItemStack spawner = plugin.getSpawnerManager().createSpawnerItem(entityType);
                        p.getInventory().addItem(spawner);
                    } catch (Exception ex) {
                        ItemStack give = new ItemStack(item.material, item.amount);
                        p.getInventory().addItem(give);
                    }
                } else {
                    ItemStack give = new ItemStack(item.material, item.amount);
                    p.getInventory().addItem(give);
                }
                
                p.sendMessage("");
                p.sendMessage("§8┃ §d§lSHARD SHOP §8┃ §a§l✓ GEKAUFT!");
                p.sendMessage("§8┃ §7Item§8: §f" + item.name + " §8x" + item.amount);
                p.sendMessage("§8┃ §7Preis§8: §d" + item.price + " Shards");
                p.sendMessage("§8┃ §7Neue Shards§8: §d" + plugin.getShards().getShards(p.getUniqueId()));
                p.sendMessage("");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            }
        } else {
            // Geld-Kauf
            double balance = plugin.getEconomy().getBalance(p.getUniqueId());
            if (balance < item.price) {
                p.sendMessage("");
                p.sendMessage("§8┃ §c§l✖ SCHUL SHOP §8┃ §cNicht genug Geld!");
                p.sendMessage("§8┃ §7Benötigt§8: §e$" + item.price);
                p.sendMessage("§8┃ §7Dein Geld§8: §e$" + String.format("%.2f", balance));
                p.sendMessage("§8┃ §7Fehlt§8: §c$" + String.format("%.2f", item.price - balance));
                p.sendMessage("");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            
            if (plugin.getEconomy().withdraw(p.getUniqueId(), item.price)) {
                ItemStack give = new ItemStack(item.material, item.amount);
                java.util.HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(give);
                
                if (!leftover.isEmpty()) {
                    for (ItemStack left : leftover.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), left);
                    }
                    p.sendMessage("§8┃ §e⚠ Inventar voll! Items gedroppt.");
                }
                
                p.sendMessage("");
                p.sendMessage("§8┃ §6§lSCHUL SHOP §8┃ §a§l✓ GEKAUFT!");
                p.sendMessage("§8┃ §7Item§8: §f" + item.name + " §8x" + item.amount);
                p.sendMessage("§8┃ §7Preis§8: §e$" + item.price);
                p.sendMessage("§8┃ §7Neuer Kontostand§8: §a$" + String.format("%.2f", plugin.getEconomy().getBalance(p.getUniqueId())));
                p.sendMessage("");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopGUI_NEW)) return;
        e.setCancelled(true);
    }
}
