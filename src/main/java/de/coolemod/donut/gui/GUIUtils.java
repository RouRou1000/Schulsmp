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

/**
 * Erweiterte GUI-Hilfen: dekorative Füller, Navigationselemente und standardisierte Menüs
 */
public class GUIUtils {
    public static Inventory create(String title, int size) {
        int s = Math.max(1, Math.min(6, size/9)) * 9; // sicherstellen, dass size/9 in [1,6]
        return Bukkit.createInventory(null, s, title);
    }

    public static Inventory createMenu(String title, int rows) {
        int r = Math.max(1, Math.min(6, rows));
        return Bukkit.createInventory(null, r * 9, title);
    }

    public static void open(Player p, Inventory inv) { p.openInventory(inv); }

    public static void safeGive(Player p, ItemStack item) {
        if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), item);
        else p.getInventory().addItem(item);
    }

    public static ItemStack filler(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) { m.setDisplayName(name); it.setItemMeta(m); }
        return it;
    }

    public static void fillBorders(Inventory inv, DonutPlugin plugin) {
        ItemStack glass = filler(Material.GRAY_STAINED_GLASS_PANE, "§7");
        int size = inv.getSize();
        // obere und untere Reihe + Seiten
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == (size/9 - 1) || col == 0 || col == 8) {
                if (inv.getItem(i) == null) inv.setItem(i, glass);
            }
        }
    }

    public static ItemStack navItem(DonutPlugin plugin, Material mat, String name, String action, int page) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            NamespacedKey key = new NamespacedKey(plugin, "donut_gui_action");
            m.getPersistentDataContainer().set(key, PersistentDataType.STRING, action);
            NamespacedKey keyPage = new NamespacedKey(plugin, "donut_gui_page");
            m.getPersistentDataContainer().set(keyPage, PersistentDataType.INTEGER, page);
            it.setItemMeta(m);
        }
        return it;
    }
}
