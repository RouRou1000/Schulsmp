package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.SellMultiplierManager;
import de.coolemod.donut.managers.SellMultiplierManager.SellCategory;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * GUI für /sellmulti - Zeigt Sell-Multiplikatoren pro Kategorie mit Fortschrittsbalken.
 */
public class SellMultiGUI {
    private final DonutPlugin plugin;

    private static final double[][] TIERS = {
            {1_000_000, 1.1},
            {3_000_000, 1.2},
            {5_000_000, 1.3},
            {10_000_000, 1.4},
            {15_000_000, 1.5},
            {30_000_000, 1.6},
            {50_000_000, 1.7},
            {70_000_000, 1.8},
            {80_000_000, 1.9},
            {100_000_000, 2.0},
            {1_000_000_000, 3.0},
            {10_000_000_000.0, 4.0},
    };

    public SellMultiGUI(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        Inventory inv = GUIUtils.createMenu("§e§l⬆ SELL MULTI", 5);
        GUIUtils.fillBorders(inv, plugin);

        SellMultiplierManager mgr = plugin.getSellMultiplier();
        UUID uuid = p.getUniqueId();

        // Header
        ItemStack header = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta hm = header.getItemMeta();
        hm.setDisplayName("§e§l⬆ §6§lSELL MULTIPLIKATOREN");
        List<String> hl = new ArrayList<>();
        hl.add("§8────────────────");
        hl.add("§7Je mehr du in einer Kategorie");
        hl.add("§7verkaufst, desto höher dein Multi!");
        hl.add("§8────────────────");
        hm.setLore(hl);
        header.setItemMeta(hm);
        inv.setItem(4, header);

        // Kategorien: Row 1 (slots 10-16) = 7, Row 2 (slots 19-20) = 2
        Material[] icons = {
                Material.WHEAT,           // CROPS
                Material.STONE,           // BLOCKS
                Material.DIAMOND,         // ORES
                Material.OAK_LOG,         // WOOD
                Material.BONE,            // MOB_DROPS
                Material.COOKED_BEEF,     // FOOD
                Material.IRON_PICKAXE,    // TOOLS
                Material.NETHER_STAR,     // SPECIAL
                Material.CHEST,           // OTHER
        };

        SellCategory[] cats = SellCategory.values();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20};

        for (int i = 0; i < cats.length && i < slots.length; i++) {
            SellCategory cat = cats[i];
            double sold = mgr.getTotalSold(uuid, cat);
            double multi = mgr.getMultiplier(uuid, cat);

            ItemStack item = new ItemStack(icons[i]);
            ItemMeta meta = item.getItemMeta();

            String multiColor = multi >= 2.0 ? "§6" : multi > 1.0 ? "§e" : "§7";
            meta.setDisplayName(cat.color + "§l" + cat.displayName + " " + multiColor + SellMultiplierManager.formatMultiplier(multi));

            List<String> lore = new ArrayList<>();
            lore.add("§8────────────────");
            lore.add("§7Verkauft: §f" + NumberFormatter.formatMoney(sold));
            lore.add("§7Multi: " + multiColor + SellMultiplierManager.formatMultiplier(multi));
            lore.add("");
            lore.add("§7" + getProgressBar(sold));
            lore.add("");

            String nextInfo = getNextTierLine(sold);
            if (nextInfo != null) {
                lore.add("§7Nächstes: §f" + nextInfo);
            } else {
                lore.add("§d✦ Maximaler Multi erreicht!");
            }
            lore.add("§8────────────────");

            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(slots[i], item);
        }

        // Tier-Übersicht
        ItemStack tierInfo = new ItemStack(Material.BOOK);
        ItemMeta tm = tierInfo.getItemMeta();
        tm.setDisplayName("§e§l✦ TIER ÜBERSICHT");
        List<String> tl = new ArrayList<>();
        tl.add("§8────────────────");
        tl.add("§f$1M §8→ §e1.1x");
        tl.add("§f$3M §8→ §e1.2x");
        tl.add("§f$5M §8→ §e1.3x");
        tl.add("§f$10M §8→ §e1.4x");
        tl.add("§f$15M §8→ §e1.5x");
        tl.add("§f$30M §8→ §e1.6x");
        tl.add("§f$50M §8→ §e1.7x");
        tl.add("§f$70M §8→ §e1.8x");
        tl.add("§f$80M §8→ §e1.9x");
        tl.add("§f$100M §8→ §e2.0x");
        tl.add("§f$1B §8→ §63.0x");
        tl.add("§f$10B §8→ §6§l4.0x");
        tl.add("§8────────────────");
        tm.setLore(tl);
        tierInfo.setItemMeta(tm);
        inv.setItem(22, tierInfo);

        // Close
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName("§c§lSCHLIESSEN");
        cm.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "donut_gui_action"),
                org.bukkit.persistence.PersistentDataType.STRING, "close_menu");
        close.setItemMeta(cm);
        inv.setItem(40, close);

        GUIUtils.open(p, inv);
    }

    private String getProgressBar(double sold) {
        double currentThreshold = 0;
        double nextThreshold = TIERS[0][0];

        for (int i = 0; i < TIERS.length; i++) {
            if (sold >= TIERS[i][0]) {
                currentThreshold = TIERS[i][0];
                if (i + 1 < TIERS.length) {
                    nextThreshold = TIERS[i + 1][0];
                } else {
                    return "§a■■■■■■■■■■§8■■■■■■■■■■ §e100%";
                }
            } else {
                nextThreshold = TIERS[i][0];
                break;
            }
        }

        double range = nextThreshold - currentThreshold;
        double progress = (sold - currentThreshold) / range;
        progress = Math.max(0, Math.min(1, progress));

        int filled = (int) (progress * 20);
        int empty = 20 - filled;
        int percent = (int) (progress * 100);

        return "§a" + "■".repeat(filled) + "§8" + "■".repeat(empty) + " §e" + percent + "%";
    }

    private String getNextTierLine(double sold) {
        for (double[] tier : TIERS) {
            if (sold < tier[0]) {
                double remaining = tier[0] - sold;
                return SellMultiplierManager.formatMultiplier(tier[1])
                        + " §8(§7noch §f" + NumberFormatter.formatMoney(remaining) + "§8)";
            }
        }
        return null;
    }
}
