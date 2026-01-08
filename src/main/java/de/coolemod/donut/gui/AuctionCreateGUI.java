package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Komplett neues AH Create System - simpel und funktional
 */
public class AuctionCreateGUI {
    private final DonutPlugin plugin;
    
    // Temporärer Preis-Speicher (UUID -> Preis)
    private static final Map<UUID, Double> prices = new HashMap<>();
    
    // Temporärer Item-Speicher (UUID -> Items)
    private static final Map<UUID, List<ItemStack>> items = new HashMap<>();

    public AuctionCreateGUI(DonutPlugin plugin) {
        this.plugin = plugin;
    }
    
    public static void setPrice(Player p, double price) {
        prices.put(p.getUniqueId(), price);
    }
    
    public static double getPrice(Player p) {
        return prices.getOrDefault(p.getUniqueId(), -1.0);
    }
    
    public static void clearPrice(Player p) {
        prices.remove(p.getUniqueId());
    }
    
    public static void saveItems(Player p, Inventory inv) {
        List<ItemStack> savedItems = new ArrayList<>();
        for (int i = 10; i <= 43; i++) {
            if (i % 9 == 0 || i % 9 == 8) continue;
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR && item.getType() != Material.BLACK_STAINED_GLASS_PANE) {
                savedItems.add(item.clone());
            }
        }
        if (!savedItems.isEmpty()) {
            items.put(p.getUniqueId(), savedItems);
        }
    }
    
    public static void clearItems(Player p) {
        items.remove(p.getUniqueId());
    }

    public void open(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, "§x§F§F§A§5§0§0§lA§x§F§F§B§0§0§0§lU§x§F§F§B§B§0§0§lK§x§F§F§C§6§0§0§lT§x§F§F§D§1§0§0§lI§x§F§F§D§C§0§0§lO§x§F§F§E§7§0§0§lN §x§F§F§F§2§0§0§lE§x§F§F§F§D§0§0§lR§x§F§F§F§F§0§8§lS§x§F§F§F§F§1§3§lT§x§F§F§F§F§1§E§lE§x§F§F§F§F§2§9§lL§x§F§F§F§F§3§4§lL§x§F§F§F§F§3§F§lE§x§F§F§F§F§4§A§lN");
        
        // Rahmen mit Glas füllen
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName("§8");
        glass.setItemMeta(gm);
        
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, glass);
            }
        }

        // Info
        ItemStack info = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§x§F§F§D§7§0§0§l⚡ §x§F§F§E§3§0§0§lᴀ§x§F§F§E§F§0§0§lɴ§x§F§F§F§B§0§0§lʟ§x§F§F§F§F§0§7§lᴇ§x§F§F§F§F§1§3§lɪ§x§F§F§F§F§1§F§lᴛ§x§F§F§F§F§2§B§lᴜ§x§F§F§F§F§3§7§lɴ§x§F§F§F§F§4§3§lɢ");
        List<String> il = new ArrayList<>();
        il.add("§8╔══════════════════╗");
        il.add("§x§F§F§D§0§0§0 ➊ §f§lItems einlegen");
        il.add("§x§F§F§E§0§0§0 ➋ §f§lPreis festlegen");
        il.add("§x§F§F§F§0§0§0 ➌ §f§lAuktion erstellen");
        il.add("§8╚══════════════════╝");
        im.setLore(il);
        info.setItemMeta(im);
        inv.setItem(4, info);

        // Preis-Button
        double savedPrice = getPrice(p);
        ItemStack priceBtn = new ItemStack(Material.GOLD_INGOT);
        ItemMeta pm = priceBtn.getItemMeta();
        if (savedPrice > 0) {
            pm.setDisplayName("§x§F§F§D§7§0§0§l💰 §x§F§F§E§3§5§5§lᴘ§x§F§F§E§F§7§7§lʀ§x§F§F§F§B§9§9§lᴇ§x§F§F§F§F§B§B§lɪ§x§F§F§F§F§D§D§lѕ§f§l: §a§l$" + String.format("%.2f", savedPrice));
        } else {
            pm.setDisplayName("§x§F§F§D§7§0§0§l💰 §x§F§F§E§3§5§5§lᴘ§x§F§F§E§F§7§7§lʀ§x§F§F§F§B§9§9§lᴇ§x§F§F§F§F§B§B§lɪ§x§F§F§F§F§D§D§lѕ §c§lꜰᴇѕᴛʟᴇɢᴇɴ");
        }
        NamespacedKey key = new NamespacedKey(plugin, "ah_action");
        pm.getPersistentDataContainer().set(key, PersistentDataType.STRING, "setprice");
        priceBtn.setItemMeta(pm);
        inv.setItem(49, priceBtn);

        // Erstellen-Button
        ItemStack confirm = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta cm = confirm.getItemMeta();
        cm.setDisplayName("§x§0§0§F§F§0§0§l✔ §x§5§5§F§F§5§5§lᴀ§x§7§7§F§F§7§7§lᴜ§x§9§9§F§F§9§9§lᴋ§x§B§B§F§F§B§B§lᴛ§x§D§D§F§F§D§D§lɪ§x§F§F§F§F§F§F§lᴏ§x§F§F§F§F§F§F§lɴ §a§lᴇʀѕᴛᴇʟʟᴇɴ");
        cm.getPersistentDataContainer().set(key, PersistentDataType.STRING, "confirm");
        confirm.setItemMeta(cm);
        inv.setItem(50, confirm);

        // Zurück-Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§x§F§F§D§7§0§0§l← §x§F§F§E§3§5§5§lᴢ§x§F§F§E§F§7§7§lᴜ§x§F§F§F§B§9§9§lʀ§x§F§F§F§F§B§B§lü§x§F§F§F§F§D§D§lᴄ§x§F§F§F§F§F§F§lᴋ");
        bm.getPersistentDataContainer().set(key, PersistentDataType.STRING, "back");
        back.setItemMeta(bm);
        inv.setItem(48, back);

        // Lade gespeicherte Items wieder ein
        if (items.containsKey(p.getUniqueId())) {
            List<ItemStack> savedItems = items.get(p.getUniqueId());
            int slot = 10;
            for (ItemStack item : savedItems) {
                if (slot % 9 == 0 || slot % 9 == 8) slot += 2;
                if (slot >= 44) break;
                inv.setItem(slot++, item);
            }
        }

        p.openInventory(inv);
    }
}
