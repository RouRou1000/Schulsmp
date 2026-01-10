package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Verbesserter WorthManager: unterstützt Material-Grundwerte, Namens-spezifische Werte und Enchant-Multiplikatoren.
 */
public class WorthManager {
    private final DonutPlugin plugin;
    private final Map<Material, Double> baseValues = new HashMap<>();
    private final Map<String, Double> namedValues = new HashMap<>(); // key: MATERIAL|DisplayName
    private final Map<Enchantment, Double> enchantMultipliers = new HashMap<>();

    public WorthManager(DonutPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void reload() {
        baseValues.clear();
        namedValues.clear();
        enchantMultipliers.clear();
        load();
    }

    private void load() {
        // Default-Werte für alle wichtigen Items
        loadDefaults();
        
        if (plugin.getConfig().isConfigurationSection("worth")) {
            for (String key : plugin.getConfig().getConfigurationSection("worth").getKeys(false)) {
                try {
                    Material m = Material.valueOf(key);
                    double v = plugin.getConfig().getDouble("worth." + key, 0.0);
                    baseValues.put(m, v);
                } catch (Exception ignored) {}
            }
        }

        if (plugin.getConfig().isConfigurationSection("worth-named")) {
            for (String key : plugin.getConfig().getConfigurationSection("worth-named").getKeys(false)) {
                double v = plugin.getConfig().getDouble("worth-named." + key, 0.0);
                namedValues.put(key, v); // key can be MATERIAL|DisplayName or simple KEY
            }
        }

        if (plugin.getConfig().isConfigurationSection("worth-enchant-multipliers")) {
            for (String key : plugin.getConfig().getConfigurationSection("worth-enchant-multipliers").getKeys(false)) {
                try {
                    Enchantment e = Enchantment.getByName(key);
                    if (e != null) {
                        double v = plugin.getConfig().getDouble("worth-enchant-multipliers." + key, 0.0);
                        enchantMultipliers.put(e, v);
                    }
                } catch (Exception ignored) {}
            }
        }

        // Fill every remaining Material with a reasonable fallback so all items are sellable.
        applyFallbacks();
    }

    public double getWorth(Material m) {
        return baseValues.getOrDefault(m, 0.0);
    }

    public double getWorth(ItemStack is) {
        if (is == null) return 0.0;
        double worth = getWorth(is.getType());
        // Check for named key in PDC
        try {
            ItemMeta meta = is.getItemMeta();
            if (meta != null && meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_named_worth"), PersistentDataType.STRING)) {
                String k = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "donut_named_worth"), PersistentDataType.STRING);
                if (k != null && namedValues.containsKey(k)) return namedValues.get(k);
            }
        } catch (Exception ignored) {}
        // Check MATERIAL|DisplayName mapping
        if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
            String key = is.getType().name() + "|" + is.getItemMeta().getDisplayName();
            if (namedValues.containsKey(key)) return namedValues.get(key);
        }
        // Add enchantment multipliers
        if (is.getEnchantments() != null && !is.getEnchantments().isEmpty()) {
            for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> e : is.getEnchantments().entrySet()) {
                Enchantment ench = e.getKey();
                int lvl = e.getValue();
                double mult = enchantMultipliers.getOrDefault(ench, 0.0);
                worth += mult * lvl;
            }
        }
        return worth;
    }

    private void loadDefaults() {
        // Erze & Barren
        baseValues.put(Material.COAL, 1.0);
        baseValues.put(Material.COPPER_INGOT, 2.0);
        baseValues.put(Material.IRON_INGOT, 5.0);
        baseValues.put(Material.GOLD_INGOT, 10.0);
        baseValues.put(Material.DIAMOND, 50.0);
        baseValues.put(Material.EMERALD, 75.0);
        baseValues.put(Material.NETHERITE_INGOT, 200.0);
        baseValues.put(Material.NETHERITE_SCRAP, 80.0);
        baseValues.put(Material.RAW_IRON, 3.0);
        baseValues.put(Material.RAW_GOLD, 6.0);
        baseValues.put(Material.RAW_COPPER, 1.5);
        
        // Blöcke
        baseValues.put(Material.COAL_BLOCK, 9.0);
        baseValues.put(Material.IRON_BLOCK, 45.0);
        baseValues.put(Material.GOLD_BLOCK, 90.0);
        baseValues.put(Material.DIAMOND_BLOCK, 450.0);
        baseValues.put(Material.EMERALD_BLOCK, 675.0);
        baseValues.put(Material.NETHERITE_BLOCK, 1800.0);
        
        // Edelsteine
        baseValues.put(Material.LAPIS_LAZULI, 2.0);
        baseValues.put(Material.REDSTONE, 1.5);
        baseValues.put(Material.QUARTZ, 3.0);
        baseValues.put(Material.AMETHYST_SHARD, 4.0);
        
        // Nether Items
        baseValues.put(Material.NETHER_STAR, 500.0);
        baseValues.put(Material.BLAZE_ROD, 15.0);
        baseValues.put(Material.ENDER_PEARL, 20.0);
        baseValues.put(Material.GHAST_TEAR, 25.0);
        baseValues.put(Material.MAGMA_CREAM, 8.0);
        
        // Mob Drops
        baseValues.put(Material.LEATHER, 2.0);
        baseValues.put(Material.FEATHER, 1.0);
        baseValues.put(Material.STRING, 1.0);
        baseValues.put(Material.SLIME_BALL, 5.0);
        baseValues.put(Material.GUNPOWDER, 3.0);
        baseValues.put(Material.BONE, 1.5);
        baseValues.put(Material.SPIDER_EYE, 2.0);
        baseValues.put(Material.ROTTEN_FLESH, 0.5);
        
        // Holz & Planken
        baseValues.put(Material.OAK_LOG, 0.5);
        baseValues.put(Material.SPRUCE_LOG, 0.5);
        baseValues.put(Material.BIRCH_LOG, 0.5);
        baseValues.put(Material.JUNGLE_LOG, 0.6);
        baseValues.put(Material.ACACIA_LOG, 0.6);
        baseValues.put(Material.DARK_OAK_LOG, 0.6);
        baseValues.put(Material.MANGROVE_LOG, 0.6);
        baseValues.put(Material.CHERRY_LOG, 0.7);
        baseValues.put(Material.BAMBOO_BLOCK, 0.4);
        
        // Stein & Erze
        baseValues.put(Material.COBBLESTONE, 0.1);
        baseValues.put(Material.STONE, 0.15);
        baseValues.put(Material.DEEPSLATE, 0.2);
        baseValues.put(Material.NETHERRACK, 0.1);
        baseValues.put(Material.END_STONE, 0.3);
        baseValues.put(Material.OBSIDIAN, 5.0);
        baseValues.put(Material.CRYING_OBSIDIAN, 8.0);
        
        // Nahrung
        baseValues.put(Material.COOKED_BEEF, 3.0);
        baseValues.put(Material.COOKED_PORKCHOP, 3.0);
        baseValues.put(Material.COOKED_CHICKEN, 2.5);
        baseValues.put(Material.COOKED_MUTTON, 2.5);
        baseValues.put(Material.BREAD, 1.5);
        baseValues.put(Material.GOLDEN_APPLE, 50.0);
        baseValues.put(Material.ENCHANTED_GOLDEN_APPLE, 500.0);
        
        // Spezial Items
        baseValues.put(Material.TOTEM_OF_UNDYING, 300.0);
        baseValues.put(Material.ELYTRA, 1000.0);
        baseValues.put(Material.TRIDENT, 200.0);
        baseValues.put(Material.SHULKER_BOX, 150.0);
        baseValues.put(Material.BEACON, 2000.0);
        baseValues.put(Material.DRAGON_HEAD, 500.0);
        baseValues.put(Material.DRAGON_EGG, 5000.0);
    }

    /**
     * Ensure every Material gets at least a fallback price so all items are sellable.
     */
    private void applyFallbacks() {
        boolean changed = false;
        for (Material material : Material.values()) {
            if (baseValues.containsKey(material)) continue;
            double fallback = computeFallback(material);
            baseValues.put(material, fallback);

            // Also persist into config if not present so config.yml gains every item with a price.
            String path = "worth." + material.name();
            if (!plugin.getConfig().isSet(path)) {
                plugin.getConfig().set(path, fallback);
                changed = true;
            }
        }

        if (changed) {
            plugin.saveConfig();
        }
    }

    private double computeFallback(Material material) {
        // Non-sellable / special cases
        if (material.isAir()) return 0.0;
        String name = material.name();
        if (name.contains("SPAWN_EGG")) return 0.0; // avoid spawner egg economy breaking
        if (name.contains("COMMAND_BLOCK") || name.contains("STRUCTURE") || name.contains("JIGSAW") ||
                name.contains("DEBUG_STICK") || name.contains("BARRIER") || name.contains("KNOWLEDGE_BOOK") ||
                name.contains("OPERATOR")) {
            return 0.0;
        }

        // Wood family aligned to user scale (16)
        if (name.contains("_LOG") || name.contains("_STEM") || name.contains("_HYPHAE") || name.contains("_PLANKS") ||
                name.contains("BAMBOO_BLOCK") || name.contains("BAMBOO_PLANKS") || name.contains("_WOOD") ||
                name.contains("STRIPPED_")) {
            return 16.0;
        }

        // Common blocks: keep cheap (1) to match config scale
        if (material.isBlock()) {
            // Glass/terracotta/concrete variants usually at 2-4 in config; use 2 as safe midpoint
            if (name.contains("GLASS") || name.contains("TERRACOTTA") || name.contains("CONCRETE") || name.contains("WOOL") ||
                    name.contains("CARPET") || name.contains("BANNER") || name.contains("CANDLE") || name.contains("COPPER") ||
                    name.contains("TUFF") || name.contains("BRICK") || name.contains("STONE") || name.contains("DEEPSLATE") ||
                    name.contains("SANDSTONE") || name.contains("PRISMARINE") || name.contains("PURPUR") ||
                    name.contains("QUARTZ") || name.contains("BLACKSTONE") || name.contains("BASALT") || name.contains("OBSIDIAN")) {
                return 2.0;
            }
            return 1.0;
        }

        // Tools/armor/weapons fallback modest value
        if (name.endsWith("_SWORD") || name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") ||
                name.endsWith("_HOE") || name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || name.endsWith("_SHIELD") ||
                name.contains("TRIDENT") || name.contains("MACE") || name.contains("BOW") || name.contains("CROSSBOW")) {
            return 20.0;
        }

        // Consumables/food low value
        if (name.contains("POTION") || name.contains("STEW") || name.contains("SOUP") || name.contains("BREAD") ||
                name.contains("FISH") || name.contains("MEAT") || name.contains("COOKIE") || name.contains("PIE") ||
                name.contains("BERRY") || name.contains("CARROT") || name.contains("POTATO") || name.contains("BEETROOT") ||
                name.contains("MELON") || name.contains("PUMPKIN") || name.contains("APPLE")) {
            return 4.0;
        }

        // Default catch-all
        return 1.0;
    }

    // programmatic registry methods
    public void setBaseWorth(Material m, double value) { baseValues.put(m, value); }
    public void setNamedWorth(String key, double value) { namedValues.put(key, value); }
    public Optional<Double> getNamedWorth(String key) { return Optional.ofNullable(namedValues.get(key)); }
}
