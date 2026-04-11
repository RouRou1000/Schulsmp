package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.storage.DataManager;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Verwaltet Sell-Multiplikatoren pro Spieler und Kategorie.
 * Je mehr ein Spieler in einer Kategorie verkauft, desto höher der Multiplikator.
 */
public class SellMultiplierManager {
    private final DonutPlugin plugin;
    private final DataManager data;

    // UUID -> Category -> totalSold
    private final Map<UUID, Map<SellCategory, Double>> playerSales = new HashMap<>();

    // Tier-Stufen: threshold -> multiplier
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

    public SellMultiplierManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.data = new DataManager(plugin.getDataFolder(), "sell-multipliers.yml");
        load();
    }

    // ── Kategorien ──────────────────────────────────────────────

    public enum SellCategory {
        CROPS("Crops", "§a"),
        BLOCKS("Blocks", "§7"),
        ORES("Erze", "§b"),
        WOOD("Holz", "§6"),
        MOB_DROPS("Mob Drops", "§c"),
        FOOD("Nahrung", "§e"),
        TOOLS("Tools & Rüstung", "§d"),
        SPECIAL("Spezial", "§5"),
        OTHER("Sonstiges", "§f");

        public final String displayName;
        public final String color;

        SellCategory(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
    }

    /**
     * Bestimmt die Kategorie eines Materials.
     * Alle platzierbaren Blöcke → BLOCKS (außer Holz → WOOD).
     * Nur non-block Items fallen in die spezifischen Kategorien.
     */
    public static SellCategory categorize(Material mat) {
        String n = mat.name();

        // Wood zuerst (Logs, Planks, Stripped, etc.)
        if (n.contains("_LOG") || n.contains("_STEM") || n.contains("_HYPHAE")
                || n.contains("_PLANKS") || n.contains("_WOOD") || n.contains("STRIPPED_")
                || n.equals("BAMBOO_BLOCK") || n.equals("BAMBOO_MOSAIC")) {
            return SellCategory.WOOD;
        }

        // ALLE platzierbaren Blöcke → BLOCKS (außer Holz, das oben schon gefangen wird)
        if (mat.isBlock()) {
            return SellCategory.BLOCKS;
        }

        // ── Ab hier nur non-block Items ──

        // Crops (Items)
        if (n.equals("WHEAT") || n.equals("SUGAR_CANE") || n.equals("CARROT") || n.equals("POTATO")
                || n.equals("BEETROOT") || n.equals("MELON_SLICE") || n.equals("COCOA_BEANS")
                || n.equals("NETHER_WART") || n.equals("SWEET_BERRIES") || n.equals("GLOW_BERRIES")
                || n.equals("KELP") || n.equals("DRIED_KELP") || n.equals("BAMBOO")
                || n.equals("CHORUS_FRUIT") || n.contains("SEEDS")
                || n.equals("TORCHFLOWER") || n.equals("PITCHER_PLANT")) {
            return SellCategory.CROPS;
        }

        // Ores & Ingots (Items)
        if (n.equals("COAL") || n.equals("IRON_INGOT") || n.equals("GOLD_INGOT") || n.equals("DIAMOND")
                || n.equals("EMERALD") || n.equals("NETHERITE_INGOT") || n.equals("NETHERITE_SCRAP")
                || n.equals("RAW_IRON") || n.equals("RAW_GOLD") || n.equals("RAW_COPPER")
                || n.equals("COPPER_INGOT") || n.equals("LAPIS_LAZULI") || n.equals("REDSTONE")
                || n.equals("QUARTZ") || n.equals("AMETHYST_SHARD") || n.equals("GOLD_NUGGET")
                || n.equals("IRON_NUGGET") || n.equals("ANCIENT_DEBRIS")) {
            return SellCategory.ORES;
        }

        // Tools & Armor
        if (n.endsWith("_SWORD") || n.endsWith("_PICKAXE") || n.endsWith("_AXE")
                || n.endsWith("_SHOVEL") || n.endsWith("_HOE") || n.endsWith("_HELMET")
                || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS")
                || n.equals("SHIELD") || n.equals("BOW") || n.equals("CROSSBOW")
                || n.equals("FISHING_ROD") || n.equals("SHEARS") || n.equals("FLINT_AND_STEEL")
                || n.equals("TRIDENT") || n.equals("MACE")) {
            return SellCategory.TOOLS;
        }

        // Mob Drops (Items)
        if (n.equals("LEATHER") || n.equals("FEATHER") || n.equals("STRING")
                || n.equals("SLIME_BALL") || n.equals("GUNPOWDER") || n.equals("BONE")
                || n.equals("SPIDER_EYE") || n.equals("ROTTEN_FLESH") || n.equals("ENDER_PEARL")
                || n.equals("BLAZE_ROD") || n.equals("BLAZE_POWDER") || n.equals("GHAST_TEAR")
                || n.equals("MAGMA_CREAM") || n.equals("PHANTOM_MEMBRANE") || n.equals("RABBIT_HIDE")
                || n.equals("RABBIT_FOOT") || n.equals("INK_SAC") || n.equals("GLOW_INK_SAC")
                || n.equals("PRISMARINE_SHARD") || n.equals("PRISMARINE_CRYSTALS")
                || n.equals("SHULKER_SHELL") || n.equals("NETHER_STAR") || n.equals("HONEYCOMB")
                || n.equals("EXPERIENCE_BOTTLE") || n.equals("EGG")
                || n.equals("ARMADILLO_SCUTE") || n.equals("BREEZE_ROD")) {
            return SellCategory.MOB_DROPS;
        }

        // Food (Items)
        if (n.contains("COOKED_") || n.equals("BREAD") || n.equals("COOKIE")
                || n.equals("GOLDEN_APPLE") || n.equals("ENCHANTED_GOLDEN_APPLE")
                || n.equals("GOLDEN_CARROT") || n.equals("MUSHROOM_STEW") || n.equals("RABBIT_STEW")
                || n.equals("BAKED_POTATO") || n.equals("PUMPKIN_PIE")
                || n.equals("BEEF") || n.equals("PORKCHOP") || n.equals("CHICKEN") || n.equals("MUTTON")
                || n.equals("RABBIT") || n.equals("COD") || n.equals("SALMON") || n.equals("TROPICAL_FISH")
                || n.equals("PUFFERFISH") || n.equals("APPLE")) {
            return SellCategory.FOOD;
        }

        // Special (Items)
        if (n.equals("TOTEM_OF_UNDYING") || n.equals("ELYTRA")
                || n.equals("HEART_OF_THE_SEA") || n.equals("NAUTILUS_SHELL")
                || n.equals("ENCHANTED_BOOK") || n.equals("NAME_TAG") || n.equals("SADDLE")
                || n.contains("MUSIC_DISC") || n.contains("DISC_FRAGMENT")
                || n.contains("POTTERY_SHERD") || n.contains("BANNER_PATTERN")
                || n.contains("SMITHING_TEMPLATE")) {
            return SellCategory.SPECIAL;
        }

        return SellCategory.OTHER;
    }

    // ── Multiplikator-Logik ─────────────────────────────────────

    /**
     * Gibt den aktuellen Multiplikator für eine Kategorie zurück.
     */
    public double getMultiplier(UUID uuid, SellCategory category) {
        double totalSold = getTotalSold(uuid, category);
        double multi = 1.0;
        for (double[] tier : TIERS) {
            if (totalSold >= tier[0]) {
                multi = tier[1];
            } else {
                break;
            }
        }
        return multi;
    }

    /**
     * Gibt den Multiplikator für ein Material zurück.
     */
    public double getMultiplier(UUID uuid, Material mat) {
        return getMultiplier(uuid, categorize(mat));
    }

    /**
     * Gibt die gesamte verkaufte Summe einer Kategorie zurück.
     */
    public double getTotalSold(UUID uuid, SellCategory category) {
        Map<SellCategory, Double> cats = playerSales.get(uuid);
        if (cats == null) return 0.0;
        return cats.getOrDefault(category, 0.0);
    }

    // ── Verkauf tracken ─────────────────────────────────────────

    /**
     * Trackt einen Verkauf und berechnet den multiplizierten Gesamtwert.
     * Gibt den BONUS-Betrag zurück (Differenz zum Base-Wert).
     * Sendet automatisch Milestone-Benachrichtigungen.
     */
    public double trackSale(Player player, Material mat, double baseValue) {
        UUID uuid = player.getUniqueId();
        SellCategory category = categorize(mat);
        double oldTotal = getTotalSold(uuid, category);
        double oldMulti = getMultiplierForAmount(oldTotal);

        // Addiere den Base-Wert zu den Gesamtverkäufen
        addSale(uuid, category, baseValue);

        double newTotal = getTotalSold(uuid, category);
        double newMulti = getMultiplierForAmount(newTotal);

        // Milestone-Check
        if (newMulti > oldMulti) {
            sendMilestoneNotification(player, category, newMulti, newTotal);
        }

        // Gib den Bonus zurück (multiplied - base)
        double currentMulti = getMultiplier(uuid, category);
        return baseValue * (currentMulti - 1.0);
    }

    /**
     * Trackt einen Verkauf mehrerer Items auf einmal.
     * Gruppiert nach Kategorie, wendet Multiplikatoren an.
     * Gibt den gesamten BONUS zurück.
     */
    public double trackSaleBatch(Player player, List<ItemStack> items, 
                                  java.util.function.Function<ItemStack, Double> worthFunction) {
        UUID uuid = player.getUniqueId();

        // Gruppiere nach Kategorie
        Map<SellCategory, Double> categoryTotals = new EnumMap<>(SellCategory.class);
        for (ItemStack item : items) {
            if (item == null) continue;
            double worth = worthFunction.apply(item);
            if (worth <= 0) continue;
            double baseTotal = worth * item.getAmount();
            SellCategory cat = categorize(item.getType());
            categoryTotals.merge(cat, baseTotal, Double::sum);
        }

        double totalBonus = 0.0;
        for (Map.Entry<SellCategory, Double> entry : categoryTotals.entrySet()) {
            SellCategory cat = entry.getKey();
            double baseValue = entry.getValue();

            double oldTotal = getTotalSold(uuid, cat);
            double oldMulti = getMultiplierForAmount(oldTotal);

            addSale(uuid, cat, baseValue);

            double newTotal = getTotalSold(uuid, cat);
            double newMulti = getMultiplierForAmount(newTotal);

            if (newMulti > oldMulti) {
                sendMilestoneNotification(player, cat, newMulti, newTotal);
            }

            double currentMulti = getMultiplier(uuid, cat);
            totalBonus += baseValue * (currentMulti - 1.0);
        }

        return totalBonus;
    }

    private void addSale(UUID uuid, SellCategory category, double amount) {
        playerSales.computeIfAbsent(uuid, k -> new EnumMap<>(SellCategory.class))
                .merge(category, amount, Double::sum);
        save();
    }

    private double getMultiplierForAmount(double totalSold) {
        double multi = 1.0;
        for (double[] tier : TIERS) {
            if (totalSold >= tier[0]) {
                multi = tier[1];
            } else {
                break;
            }
        }
        return multi;
    }

    // ── Benachrichtigung ────────────────────────────────────────

    private void sendMilestoneNotification(Player player, SellCategory category, double newMulti, double totalSold) {
        player.sendMessage("");
        player.sendMessage("§8┃ §6§l⬆ SELL MULTI §8┃ §aMeilenstein erreicht!");
        player.sendMessage("§8  ▸ §7Kategorie: " + category.color + category.displayName);
        player.sendMessage("§8  ▸ §7Neuer Multiplikator: §e" + formatMultiplier(newMulti));
        player.sendMessage("§8  ▸ §7Gesamt verkauft: §f" + NumberFormatter.formatMoney(totalSold));

        // Nächster Meilenstein
        String nextInfo = getNextTierInfo(totalSold);
        if (nextInfo != null) {
            player.sendMessage("§8  ▸ §7Nächster: §f" + nextInfo);
        } else {
            player.sendMessage("§8  ▸ §d✦ Maximaler Multiplikator erreicht!");
        }
        player.sendMessage("");

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
    }

    private String getNextTierInfo(double currentTotal) {
        for (double[] tier : TIERS) {
            if (currentTotal < tier[0]) {
                return formatMultiplier(tier[1]) + " bei " + NumberFormatter.formatMoney(tier[0]);
            }
        }
        return null; // Max erreicht
    }

    public static String formatMultiplier(double multi) {
        if (multi == (int) multi) {
            return (int) multi + "x";
        }
        return String.format("%.1fx", multi);
    }

    // ── Persistenz ──────────────────────────────────────────────

    public void save() {
        FileConfiguration cfg = data.getConfig();
        for (Map.Entry<UUID, Map<SellCategory, Double>> entry : playerSales.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<SellCategory, Double> catEntry : entry.getValue().entrySet()) {
                cfg.set("sales." + uuid + "." + catEntry.getKey().name(), catEntry.getValue());
            }
        }
        data.save();
    }

    private void load() {
        FileConfiguration cfg = data.getConfig();
        if (!cfg.contains("sales")) return;

        for (String uuidStr : cfg.getConfigurationSection("sales").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Map<SellCategory, Double> cats = new EnumMap<>(SellCategory.class);
                for (String catName : cfg.getConfigurationSection("sales." + uuidStr).getKeys(false)) {
                    try {
                        SellCategory cat = SellCategory.valueOf(catName);
                        cats.put(cat, cfg.getDouble("sales." + uuidStr + "." + catName));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                playerSales.put(uuid, cats);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    // ── Info für Anzeige ────────────────────────────────────────

    /**
     * Gibt eine formatierte Übersicht der Multiplikatoren eines Spielers zurück.
     */
    public List<String> getMultiplierInfo(UUID uuid) {
        List<String> lines = new ArrayList<>();
        for (SellCategory cat : SellCategory.values()) {
            double multi = getMultiplier(uuid, cat);
            double sold = getTotalSold(uuid, cat);
            if (sold > 0 || multi > 1.0) {
                lines.add(cat.color + cat.displayName + " §8» §e" + formatMultiplier(multi)
                        + " §8(§7" + NumberFormatter.formatMoney(sold) + " verkauft§8)");
            }
        }
        return lines;
    }
}
