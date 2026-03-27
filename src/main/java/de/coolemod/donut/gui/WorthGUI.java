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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Paginierte GUI für /worth - zeigt alle Item-Preise sortiert nach Wert
 */
public class WorthGUI {
    private final DonutPlugin plugin;
    private static final int ITEMS_PER_PAGE = 36; // 4 Reihen à 9 Items (Slots 9-44)

    public WorthGUI(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p, int page) {
        List<Map.Entry<Material, Double>> sorted = getSortedEntries();

        int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / ITEMS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = "§e§l" + toSmallCaps("ITEM PRICES") + " §8(" + toSmallCaps("PAGE") + " " + (page + 1) + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Borders füllen
        GUIUtils.fillBorders(inv, plugin);

        // Items für diese Seite einfügen
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, sorted.size());

        for (int i = start; i < end; i++) {
            Map.Entry<Material, Double> entry = sorted.get(i);
            Material mat = entry.getKey();
            double price = entry.getValue();

            if (!mat.isItem()) continue;

            int slot = 9 + (i - start); // Slots 9-44
            // Überspringe Border-Slots (Spalte 0 und 8)
            int row = (slot - 9) / 9;
            int col = (slot - 9) % 9;
            // Wir nutzen die inneren 7 Slots pro Reihe (1-7)
            // Neuberechnung: 7 items pro Reihe
            int itemIndex = i - start;
            row = itemIndex / 7;
            col = (itemIndex % 7) + 1; // Spalten 1-7
            slot = (row + 1) * 9 + col; // Reihe 1-4, Spalte 1-7

            if (slot >= 45) break; // Nicht in die untere Border-Reihe

            ItemStack display = new ItemStack(mat);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                String itemName = formatMaterialName(mat);
                meta.setDisplayName("§e§l" + itemName);
                List<String> lore = new ArrayList<>();
                lore.add("§8┃");
                lore.add("§8┃ §7Preis§8: §a$" + formatPrice(price));
                lore.add("§8┃");
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(slot, display);
        }

        // Sortier-Button (oben mitte)
        ItemStack sortItem = new ItemStack(Material.HOPPER);
        ItemMeta sortMeta = sortItem.getItemMeta();
        sortMeta.setDisplayName("§e§lsᴏʀᴛ");
        sortMeta.setLore(Arrays.asList(
            "§8┃",
            "§8┃ §7Sortiert nach §ePreis",
            "§8┃ §7Teuerste zuerst",
            "§8┃"
        ));
        sortItem.setItemMeta(sortMeta);
        inv.setItem(4, sortItem);

        // Seiten-Info (unten mitte)
        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        pageMeta.setDisplayName("§e§l📄 " + toSmallCaps("SEITE") + " §f" + (page + 1) + " §7/ §f" + totalPages);
        pageMeta.setLore(Arrays.asList(
            "§8┃",
            "§8┃ §7Gesamt§8: §e" + sorted.size() + " Items",
            "§8┃"
        ));
        pageInfo.setItemMeta(pageMeta);
        inv.setItem(49, pageInfo);

        // Vorherige Seite (unten links)
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName("§e§l◄ " + toSmallCaps("VORHERIGE SEITE"));
            prevMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "worth_page"),
                PersistentDataType.INTEGER,
                page - 1);
            prev.setItemMeta(prevMeta);
            inv.setItem(45, prev);
        }

        // Nächste Seite (unten rechts)
        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("§e§l" + toSmallCaps("NÄCHSTE SEITE") + " ►");
            nextMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "worth_page"),
                PersistentDataType.INTEGER,
                page + 1);
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }

        // Schließen
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§c§l✖ " + toSmallCaps("SCHLIESSEN"));
        close.setItemMeta(closeMeta);
        inv.setItem(48, close);

        p.openInventory(inv);
    }

    private List<Map.Entry<Material, Double>> getSortedEntries() {
        return plugin.getWorthManager().getBaseValues().entrySet().stream()
            .filter(e -> e.getValue() > 0 && e.getKey().isItem())
            .sorted(Map.Entry.<Material, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
    }

    private String formatPrice(double price) {
        if (price >= 1_000_000_000) {
            return String.format("%.2fB", price / 1_000_000_000);
        } else if (price >= 1_000_000) {
            return String.format("%.2fM", price / 1_000_000);
        } else if (price >= 1_000) {
            return String.format("%.1fK", price / 1_000);
        } else {
            if (price == (long) price) {
                return String.format("%.0f", price);
            }
            return String.format("%.2f", price);
        }
    }

    private String formatMaterialName(Material mat) {
        String name = mat.name().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    public boolean isWorthGUI(String title) {
        return title != null && title.contains(toSmallCaps("ITEM PRICES"));
    }

    private String toSmallCaps(String text) {
        return text.replace("A", "ᴀ").replace("B", "ʙ").replace("C", "ᴄ")
            .replace("D", "ᴅ").replace("E", "ᴇ").replace("F", "ғ")
            .replace("G", "ɢ").replace("H", "ʜ").replace("I", "ɪ")
            .replace("J", "ᴊ").replace("K", "ᴋ").replace("L", "ʟ")
            .replace("M", "ᴍ").replace("N", "ɴ").replace("O", "ᴏ")
            .replace("P", "ᴘ").replace("Q", "ǫ").replace("R", "ʀ")
            .replace("S", "s").replace("T", "ᴛ").replace("U", "ᴜ")
            .replace("V", "ᴠ").replace("W", "ᴡ").replace("X", "x")
            .replace("Y", "ʏ").replace("Z", "ᴢ");
    }
}
