package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
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
    private static final int ITEMS_PER_PAGE = 28; // 4 Reihen à 7 Items (Spalten 1-7)
    private static final Map<UUID, String> SEARCH_QUERIES = new HashMap<>();

    private static final Map<String, String> ENCHANT_NAMES = Map.ofEntries(
        Map.entry("sharpness",             "Sharpness"),
        Map.entry("smite",                 "Smite"),
        Map.entry("bane_of_arthropods",    "Bane of Arthropods"),
        Map.entry("knockback",             "Knockback"),
        Map.entry("fire_aspect",           "Fire Aspect"),
        Map.entry("looting",               "Looting"),
        Map.entry("sweeping_edge",         "Sweeping Edge"),
        Map.entry("efficiency",            "Efficiency"),
        Map.entry("unbreaking",            "Unbreaking"),
        Map.entry("silk_touch",            "Silk Touch"),
        Map.entry("fortune",               "Fortune"),
        Map.entry("power",                 "Power"),
        Map.entry("punch",                 "Punch"),
        Map.entry("flame",                 "Flame"),
        Map.entry("infinity",              "Infinity"),
        Map.entry("luck_of_the_sea",       "Luck of the Sea"),
        Map.entry("lure",                  "Lure"),
        Map.entry("loyalty",               "Loyalty"),
        Map.entry("channeling",            "Channeling"),
        Map.entry("impaling",              "Impaling"),
        Map.entry("riptide",               "Riptide"),
        Map.entry("multishot",             "Multishot"),
        Map.entry("piercing",              "Piercing"),
        Map.entry("quick_charge",          "Quick Charge"),
        Map.entry("protection",            "Protection"),
        Map.entry("fire_protection",       "Fire Protection"),
        Map.entry("feather_falling",       "Feather Falling"),
        Map.entry("blast_protection",      "Blast Protection"),
        Map.entry("projectile_protection", "Projectile Protection"),
        Map.entry("respiration",           "Respiration"),
        Map.entry("aqua_affinity",         "Aqua Affinity"),
        Map.entry("thorns",                "Thorns"),
        Map.entry("depth_strider",         "Depth Strider"),
        Map.entry("frost_walker",          "Frost Walker"),
        Map.entry("mending",               "Mending"),
        Map.entry("soul_speed",            "Soul Speed"),
        Map.entry("swift_sneak",           "Swift Sneak"),
        Map.entry("density",               "Density"),
        Map.entry("breach",                "Breach"),
        Map.entry("wind_burst",            "Wind Burst"),
        Map.entry("binding_curse",         "Curse of Binding"),
        Map.entry("vanishing_curse",       "Curse of Vanishing")
    );

    private static final Set<String> UNOBTAINABLE = Set.of(
        "BARRIER", "STRUCTURE_BLOCK", "STRUCTURE_VOID", "JIGSAW", "LIGHT",
        "DEBUG_STICK", "KNOWLEDGE_BOOK", "BEDROCK", "PETRIFIED_OAK_SLAB",
        "REINFORCED_DEEPSLATE", "FROGSPAWN", "FROSTED_ICE", "SPAWNER",
        "BUDDING_AMETHYST", "END_PORTAL_FRAME", "TRIAL_SPAWNER", "VAULT",
        "FARMLAND", "PLAYER_HEAD", "PLAYER_WALL_HEAD", "COMMAND_BLOCK_MINECART"
    );

    public WorthGUI(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p, int page) {
        List<Map.Entry<Material, Double>> sorted = getSortedEntries(getSearchQuery(p.getUniqueId()));

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

        ItemStack searchItem = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchItem.getItemMeta();
        if (searchMeta != null) {
            String query = getSearchQuery(p.getUniqueId());
            searchMeta.setDisplayName("§e§l🔍 " + toSmallCaps("SUCHEN"));
            List<String> searchLore = new ArrayList<>();
            searchLore.add("§8┃");
            if (query != null && !query.isBlank()) {
                searchLore.add("§8┃ §7Aktive Suche§8: §f\"" + query + "\"");
                searchLore.add("§8┃");
                searchLore.add("§8┃ §aLinksklick§8: §7Neu suchen");
                searchLore.add("§8┃ §cRechtsklick§8: §7Suche löschen");
            } else {
                searchLore.add("§8┃ §7Suche nach Item-Namen");
                searchLore.add("§8┃");
                searchLore.add("§8┃ §eKlicken zum Suchen");
            }
            searchLore.add("§8┃");
            searchMeta.setLore(searchLore);
            searchItem.setItemMeta(searchMeta);
        }
        inv.setItem(50, searchItem);

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

        // Verzauberungs-Tab Button (slot 46)
        ItemStack enchBtn = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta enchMeta = enchBtn.getItemMeta();
        enchMeta.setDisplayName("§d§l✨ " + toSmallCaps("VERZAUBERUNGEN"));
        enchMeta.setLore(Arrays.asList(
            "§8┃",
            "§8┃ §7Zeigt alle Verzauberungs-Werte,",
            "§8┃ §7die beim Verkauf addiert werden.",
            "§8┃"
        ));
        enchMeta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "worth_tab"), PersistentDataType.STRING, "enchants");
        enchBtn.setItemMeta(enchMeta);
        inv.setItem(46, enchBtn);

        p.openInventory(inv);
    }

    private List<Map.Entry<Material, Double>> getSortedEntries(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return plugin.getWorthManager().getBaseValues().entrySet().stream()
            .filter(e -> e.getValue() > 0 && e.getKey().isItem())
            .filter(e -> !isUnobtainable(e.getKey()))
            .filter(e -> normalizedQuery.isEmpty() || formatMaterialName(e.getKey()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || e.getKey().name().toLowerCase(Locale.ROOT).contains(normalizedQuery))
            .sorted(Map.Entry.<Material, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
    }

    public static void setSearchQuery(UUID playerId, String query) {
        if (query == null || query.isBlank()) {
            SEARCH_QUERIES.remove(playerId);
            return;
        }
        SEARCH_QUERIES.put(playerId, query.trim());
    }

    public static void clearSearchQuery(UUID playerId) {
        SEARCH_QUERIES.remove(playerId);
    }

    public static String getSearchQuery(UUID playerId) {
        return SEARCH_QUERIES.get(playerId);
    }

    private static boolean isUnobtainable(Material mat) {
        String name = mat.name();
        return UNOBTAINABLE.contains(name)
            || name.contains("SPAWN_EGG")
            || name.contains("COMMAND_BLOCK")
            || name.startsWith("INFESTED_")
            || name.endsWith("_WALL_HEAD")
            || name.endsWith("_WALL_SKULL")
            || name.endsWith("_WALL_BANNER")
            || name.endsWith("_WALL_SIGN")
            || name.endsWith("_WALL_HANGING_SIGN")
            || name.endsWith("_WALL_TORCH")
            || name.endsWith("_WALL_FAN");
    }

    private String formatPrice(double price) {
        return de.coolemod.donut.utils.NumberFormatter.format(price);
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

    public void openEnchants(Player p, int page) {
        Map<Enchantment, Double> multipliers = plugin.getWorthManager().getEnchantMultipliers();
        List<Map.Entry<Enchantment, Double>> sorted = multipliers.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .sorted(Map.Entry.<Enchantment, Double>comparingByValue().reversed())
            .collect(Collectors.toList());

        int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / 28));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        String title = "§d§l" + toSmallCaps("ENCHANT PRICES")
            + " §8(" + toSmallCaps("SEITE") + " " + (page + 1) + "/" + totalPages + ")";
        Inventory inv = Bukkit.createInventory(null, 54, title);
        GUIUtils.fillBorders(inv, plugin);

        // Header
        ItemStack header = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta hm = header.getItemMeta();
        hm.setDisplayName("§d§l✨ " + toSmallCaps("VERZAUBERUNGS-PREISE"));
        hm.setLore(Arrays.asList(
            "§8┃",
            "§8┃ §7Pro Level zum Item-Preis addiert.",
            "§8┃ §7Sortiert nach höchstem Wert.",
            "§8┃"
        ));
        hm.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING, "worth_enchant_display");
        header.setItemMeta(hm);
        inv.setItem(4, header);

        int start = page * 28;
        int end   = Math.min(start + 28, sorted.size());
        int[] slots = {10,11,12,13,14,15,16, 19,20,21,22,23,24,25, 28,29,30,31,32,33,34, 37,38,39,40,41,42,43};

        for (int i = start; i < end; i++) {
            Map.Entry<Enchantment, Double> entry = sorted.get(i);
            Enchantment ench = entry.getKey();
            double mult     = entry.getValue();
            int    maxLevel = ench.getMaxLevel();

            ItemStack display = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta  meta    = display.getItemMeta();
            String    name    = formatEnchantName(ench);
            meta.setDisplayName("§d§l" + name);
            List<String> lore = new ArrayList<>();
            lore.add("§8┃");
            lore.add("§8┃ §7Pro Level§8: §a+$" + formatPrice(mult));
            lore.add("§8┃ §7Max §8(" + toRoman(maxLevel) + ")§8: §a+$" + formatPrice(mult * maxLevel));
            lore.add("§8┃");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING, "worth_enchant_display");
            display.setItemMeta(meta);
            inv.setItem(slots[i - start], display);
        }

        // Zurück zu Items
        ItemStack back     = new ItemStack(Material.ARROW);
        ItemMeta  backMeta = back.getItemMeta();
        backMeta.setDisplayName("§e§l◄ " + toSmallCaps("ITEM PREISE"));
        backMeta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "worth_tab"), PersistentDataType.STRING, "items");
        back.setItemMeta(backMeta);
        inv.setItem(45, back);

        // Vorherige Seite
        if (page > 0) {
            ItemStack prev     = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta  prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName("§e§l◄ " + toSmallCaps("VORHERIGE SEITE"));
            prevMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "worth_page"), PersistentDataType.INTEGER, page - 1);
            prev.setItemMeta(prevMeta);
            inv.setItem(46, prev);
        }

        // Schließen
        ItemStack close     = new ItemStack(Material.BARRIER);
        ItemMeta  closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§c§l✖ " + toSmallCaps("SCHLIESSEN"));
        close.setItemMeta(closeMeta);
        inv.setItem(48, close);

        // Nächste Seite
        if (page < totalPages - 1) {
            ItemStack next     = new ItemStack(Material.SPECTRAL_ARROW);
            ItemMeta  nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("§e§l" + toSmallCaps("NÄCHSTE SEITE") + " ►");
            nextMeta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "worth_page"), PersistentDataType.INTEGER, page + 1);
            next.setItemMeta(nextMeta);
            inv.setItem(53, next);
        }

        p.openInventory(inv);
    }

    public boolean isEnchantWorthGUI(String title) {
        return title != null && title.contains(toSmallCaps("ENCHANT PRICES"));
    }

    private String formatEnchantName(Enchantment ench) {
        String key = ench.getKey().getKey();
        if (ENCHANT_NAMES.containsKey(key)) return ENCHANT_NAMES.get(key);
        // Fallback: capitalize each word
        String[] words = key.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!word.isEmpty()) sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    private String toRoman(int n) {
        return switch (n) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            default -> String.valueOf(n);
        };
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
