package de.coolemod.donut.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Kleine Helfer zum Parsen/Serialisieren einfacher Item-Definitionen
 * Unterstützt Formate wie:
 * - MATERIAL
 * - MATERIAL:amount
 */
public class ItemUtil {
    public static ItemStack fromSimpleString(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            String[] parts = s.split(":");
            Material m = Material.valueOf(parts[0]);
            int amt = 1;
            if (parts.length >= 2) amt = Integer.parseInt(parts[1]);
            ItemStack it = new ItemStack(m, amt);
            return it;
        } catch (Exception ex) {
            return null;
        }
    }

    public static String toSimpleString(ItemStack is) {
        if (is == null) return "";
        return is.getType().name() + ":" + is.getAmount();
    }

    public static String nameOf(ItemStack is) {
        if (is == null) return "";
        ItemMeta m = is.getItemMeta();
        if (m != null && m.hasDisplayName()) return m.getDisplayName();
        return is.getType().name();
    }
}