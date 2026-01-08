package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Sell-GUI: Spieler legt Items rein, die verkauft werden sollen
 */
public class SellGUI {
    private final DonutPlugin plugin;
    
    public SellGUI(DonutPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player p) {
        Inventory inv = GUIUtils.createMenu("§a§l$ ɪᴛᴇᴍѕ ᴠᴇʀᴋᴀᴜꜰᴇɴ", 6);
        
        // Info-Item
        ItemStack info = new ItemStack(Material.SUNFLOWER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§e§lᴡɪᴇ ꜰᴜɴᴋᴛɪᴏɴɪᴇʀᴛ'ѕ?");
        List<String> il = new ArrayList<>();
        il.add("§8────────────────");
        il.add("§71. Lege Items in die Slots");
        il.add("§72. Klicke auf §aVerkaufen");
        il.add("§73. Erhalte sofort Geld!");
        il.add("§8────────────────");
        il.add("§7Wert wird automatisch");
        il.add("§7berechnet.");
        im.setLore(il);
        info.setItemMeta(im);
        inv.setItem(4, info);
        
        // Verkaufen-Button
        ItemStack sellBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta sm = sellBtn.getItemMeta();
        sm.setDisplayName("§a§l✔ ᴠᴇʀᴋᴀᴜꜰᴇɴ");
        List<String> sl = new ArrayList<>();
        sl.add("§8────────────────");
        sl.add("§7Verkaufe alle Items");
        sl.add("§7in diesem Menü");
        sl.add("§8────────────────");
        sl.add("§e➤ Klick zum Verkaufen!");
        sm.setLore(sl);
        sm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "sell_items");
        sellBtn.setItemMeta(sm);
        inv.setItem(49, sellBtn);
        
        // Schließen-Button
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta cm = closeBtn.getItemMeta();
        cm.setDisplayName("§c§lѕᴄʜʟɪᴇѕѕᴇɴ");
        List<String> cl = new ArrayList<>();
        cl.add("§7Items werden zurückgegeben");
        cm.setLore(cl);
        cm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "sell_close");
        closeBtn.setItemMeta(cm);
        inv.setItem(53, closeBtn);
        
        // Worth-Preview-Button
        ItemStack worthBtn = new ItemStack(Material.GOLD_INGOT);
        ItemMeta wm = worthBtn.getItemMeta();
        wm.setDisplayName("§e§lɢᴇѕᴀᴍᴛᴡᴇʀᴛ");
        List<String> wl = new ArrayList<>();
        wl.add("§8────────────────");
        wl.add("§7Aktueller Wert:");
        wl.add("§a$0.00");
        wl.add("§8────────────────");
        wl.add("§7Wird automatisch");
        wl.add("§7aktualisiert");
        wm.setLore(wl);
        wm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "sell_worth_display");
        worthBtn.setItemMeta(wm);
        inv.setItem(45, worthBtn);
        
        // Borders (außer Slots 10-34 für Items)
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                    inv.setItem(i, GUIUtils.filler(Material.BLACK_STAINED_GLASS_PANE, "§7"));
                }
            }
        }
        
        GUIUtils.open(p, inv);
    }
    
    public static double calculateTotalWorth(DonutPlugin plugin, Inventory inv) {
        double total = 0.0;
        for (int i = 10; i <= 43; i++) {
            if (i % 9 == 0 || i % 9 == 8) continue; // Skip borders
            ItemStack is = inv.getItem(i);
            if (is == null || is.getType() == Material.AIR) continue;
            // Check if it's not a GUI item
            if (is.hasItemMeta() && is.getItemMeta().getPersistentDataContainer().has(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING)) {
                continue;
            }
            double worth = plugin.getWorthManager().getWorth(is);
            total += worth * is.getAmount();
        }
        return total;
    }
    
    public static void updateWorthDisplay(DonutPlugin plugin, Inventory inv) {
        ItemStack worthBtn = inv.getItem(45);
        if (worthBtn != null && worthBtn.getType() == Material.GOLD_INGOT) {
            double total = calculateTotalWorth(plugin, inv);
            ItemMeta wm = worthBtn.getItemMeta();
            if (wm != null) {
                List<String> wl = new ArrayList<>();
                wl.add("§8────────────────");
                wl.add("§7Aktueller Wert:");
                wl.add("§a$" + String.format("%.2f", total));
                wl.add("§8────────────────");
                wl.add("§7Wird automatisch");
                wl.add("§7aktualisiert");
                wm.setLore(wl);
                worthBtn.setItemMeta(wm);
            }
        }
    }
}
