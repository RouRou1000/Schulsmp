package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * KOMPLETT NEUER Shop mit InventoryHolder - garantiert sicher!
 */
public class ShopGUI_NEW implements InventoryHolder {
    private final DonutPlugin plugin;
    private Inventory inventory;
    
    // Shop Items mit Preis-Mapping
    private static final Map<Integer, ShopItem> MAIN_ITEMS = new HashMap<>();
    private static final Map<Integer, ShopItem> FOOD_ITEMS = new HashMap<>();
    private static final Map<Integer, ShopItem> GEAR_ITEMS = new HashMap<>();
    private static final Map<Integer, ShopItem> NETHER_ITEMS = new HashMap<>();
    private static final Map<Integer, ShopItem> SHARD_ITEMS = new HashMap<>();
    
    static {
        // Food Shop Items
        FOOD_ITEMS.put(10, new ShopItem(Material.GOLDEN_APPLE, "§6§lGolden Apple", 50, 1, "§8┃ §7Heilung + Absorption", "§8▸ §aPreis§8: §e$50"));
        FOOD_ITEMS.put(11, new ShopItem(Material.COOKED_BEEF, "§c§lSteak", 80, 16, "§8┃ §7Sättigung & Heilung", "§8▸ §aPreis§8: §e$80"));
        FOOD_ITEMS.put(12, new ShopItem(Material.GOLDEN_CARROT, "§6§lGolden Carrot", 120, 16, "§8┃ §7Beste Nahrung!", "§8▸ §aPreis§8: §e$120"));
        FOOD_ITEMS.put(13, new ShopItem(Material.APPLE, "§c§lApple", 25, 16, "§8┃ §7Basis-Nahrung", "§8▸ §aPreis§8: §e$25"));
        FOOD_ITEMS.put(14, new ShopItem(Material.CARROT, "§6§lCarrot", 20, 16, "§8┃ §7Gemüse", "§8▸ §aPreis§8: §e$20"));
        FOOD_ITEMS.put(15, new ShopItem(Material.COOKED_CHICKEN, "§f§lChicken", 60, 16, "§8┃ §7Gebratenes Huhn", "§8▸ §aPreis§8: §e$60"));
        FOOD_ITEMS.put(16, new ShopItem(Material.COOKED_PORKCHOP, "§d§lPork", 70, 16, "§8┃ §7Gebratenes Schwein", "§8▸ §aPreis§8: §e$70"));
        FOOD_ITEMS.put(19, new ShopItem(Material.COOKED_MUTTON, "§7§lMutton", 55, 16, "§8┃ §7Hammelfleisch", "§8▸ §aPreis§8: §e$55"));
        FOOD_ITEMS.put(20, new ShopItem(Material.SWEET_BERRIES, "§c§lSweet Berries", 30, 32, "§8┃ §7Süße Beeren", "§8▸ §aPreis§8: §e$30"));
        FOOD_ITEMS.put(21, new ShopItem(Material.BAKED_POTATO, "§6§lPotato", 25, 16, "§8┃ §7Gebackene Kartoffel", "§8▸ §aPreis§8: §e$25"));
        FOOD_ITEMS.put(22, new ShopItem(Material.MELON_SLICE, "§a§lMelon", 35, 32, "§8┃ §7Melonenscheiben", "§8▸ §aPreis§8: §e$35"));
        FOOD_ITEMS.put(23, new ShopItem(Material.ARROW, "§f§lArrows", 40, 64, "§8┃ §7Munition für Bögen", "§8▸ §aPreis§8: §e$40"));
        
        // Gear Shop Items
        GEAR_ITEMS.put(11, new ShopItem(Material.TOTEM_OF_UNDYING, "§6§lTotem of Undying", 5000, 1, "§8┃ §7Rettet dein Leben! §c§lSELTEN!", "§8▸ §aPreis§8: §e$5,000"));
        GEAR_ITEMS.put(13, new ShopItem(Material.GOLDEN_APPLE, "§6§lGolden Apple", 380, 8, "§8┃ §7Heilung + Absorption", "§8▸ §aPreis§8: §e$380"));
        GEAR_ITEMS.put(15, new ShopItem(Material.EXPERIENCE_BOTTLE, "§a§lXP Bottle", 250, 64, "§8┃ §7Schnelles Leveln", "§8▸ §aPreis§8: §e$250"));
        GEAR_ITEMS.put(20, new ShopItem(Material.ENDER_PEARL, "§b§lEnder Pearl", 320, 16, "§8┃ §7Teleportation", "§8▸ §aPreis§8: §e$320"));
        GEAR_ITEMS.put(24, new ShopItem(Material.ENDER_PEARL, "§b§lEnder Pearl Stack", 1200, 64, "§8┃ §7Viel Teleportation!", "§8▸ §aPreis§8: §e$1,200"));
        
        // Nether Shop Items
        NETHER_ITEMS.put(10, new ShopItem(Material.BLAZE_ROD, "§6§lBlaze Rod", 100, 16, "§8┃ §7Brauen & Craften", "§8▸ §aPreis§8: §e$100"));
        NETHER_ITEMS.put(12, new ShopItem(Material.NETHER_WART, "§c§lNether Wart", 50, 32, "§8┃ §7Basis für Tränke", "§8▸ §aPreis§8: §e$50"));
        NETHER_ITEMS.put(14, new ShopItem(Material.QUARTZ, "§f§lQuartz", 60, 64, "§8┃ §7Baumaterial", "§8▸ §aPreis§8: §e$60"));
        NETHER_ITEMS.put(16, new ShopItem(Material.MAGMA_CREAM, "§c§lMagma Cream", 80, 16, "§8┃ §7Brauzutat", "§8▸ §aPreis§8: §e$80"));
        NETHER_ITEMS.put(20, new ShopItem(Material.GLOWSTONE_DUST, "§e§lGlowstone Dust", 40, 32, "§8┃ §7Licht & Brauen", "§8▸ §aPreis§8: §e$40"));
        NETHER_ITEMS.put(22, new ShopItem(Material.GHAST_TEAR, "§f§lGhast Tear", 150, 8, "§8┃ §7Regen-Tränke", "§8▸ §aPreis§8: §e$150"));
        NETHER_ITEMS.put(24, new ShopItem(Material.NETHERITE_SCRAP, "§c§lNetherite Scrap", 2000, 1, "§8┃ §c§lSEHR SELTEN!", "§8▸ §aPreis§8: §e$2,000"));
        
        // Shard Shop Items
        SHARD_ITEMS.put(11, new ShopItem(Material.SPAWNER, "§d§lZombie Spawner", 500, 1, "ZOMBIE", "§8┃ §7Spawner für Zombies", "§8┃ §d§lSHARD-ONLY!", "§8▸ §aPreis§8: §d500 Shards"));
        SHARD_ITEMS.put(13, new ShopItem(Material.SPAWNER, "§d§lSkeleton Spawner", 500, 1, "SKELETON", "§8┃ §7Spawner für Skelette", "§8┃ §d§lSHARD-ONLY!", "§8▸ §aPreis§8: §d500 Shards"));
        SHARD_ITEMS.put(15, new ShopItem(Material.SPAWNER, "§d§lCreeper Spawner", 750, 1, "CREEPER", "§8┃ §7Spawner für Creeper", "§8┃ §d§lSEHR WERTVOLL!", "§8▸ §aPreis§8: §d750 Shards"));
        SHARD_ITEMS.put(20, new ShopItem(Material.SPAWNER, "§d§lSpider Spawner", 600, 1, "SPIDER", "§8┃ §7Spawner für Spinnen", "§8┃ §d§lSHARD-ONLY!", "§8▸ §aPreis§8: §d600 Shards"));
        SHARD_ITEMS.put(22, new ShopItem(Material.SPAWNER, "§d§lBlaze Spawner", 1000, 1, "BLAZE", "§8┃ §7Spawner für Blazes", "§8┃ §d§l§lULTRA SELTEN!", "§8▸ §aPreis§8: §d1000 Shards"));
        SHARD_ITEMS.put(24, new ShopItem(Material.SPAWNER, "§d§lIron Golem Spawner", 2000, 1, "IRON_GOLEM", "§8┃ §7Spawner für Golems", "§8┃ §d§lLEGENDÄR!", "§8▸ §aPreis§8: §d2000 Shards"));
    }
    
    public ShopGUI_NEW(DonutPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public void openMainMenu(Player p) {
        inventory = Bukkit.createInventory(this, 54, "§6§l⛃ SCHUL SHOP ⛃");
        
        // Borders
        fillBorders(inventory);
        
        // Balance
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        ItemStack balInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bi = balInfo.getItemMeta();
        bi.setDisplayName("§6§l⛃ §e§lDEIN GUTHABEN");
        bi.setLore(Arrays.asList("§8┃", "§8┃ §7Balance§8: §a$" + String.format("%.2f", balance), "§8┃", "§8▸ §7Kaufe Items mit deinem Geld!"));
        balInfo.setItemMeta(bi);
        inventory.setItem(4, balInfo);
        
        // Kategorien
        inventory.setItem(20, createButton(Material.COOKED_BEEF, "§6§l🍖 FOOD", "category_food", "§8┃ §7Nahrung und Heilung", "§8┃ §7Gaps, Steaks & mehr", "§8▸ §eKlicke zum Öffnen!"));
        inventory.setItem(22, createButton(Material.TOTEM_OF_UNDYING, "§b§l⚔ GEAR", "category_gear", "§8┃ §7Kampf-Ausrüstung", "§8┃ §7Totems, Perlen & XP", "§8▸ §eKlicke zum Öffnen!"));
        inventory.setItem(24, createButton(Material.NETHER_WART, "§c§l✦ NETHER", "category_nether", "§8┃ §7Nether-Ressourcen", "§8┃ §7Blazerods, Quarz & mehr", "§8▸ §eKlicke zum Öffnen!"));
        inventory.setItem(30, createButton(Material.END_STONE, "§5§l⬢ END §8(§c✖§8)", "category_end", "§8┃ §cGesperrt!", "§8┃ §7End-Items später verfügbar"));
        inventory.setItem(32, createButton(Material.SPAWNER, "§d§l❖ SHARD SHOP", "category_shards", "§8┃ §7Spawner & Spezial-Items", "§8┃ §7Kaufe mit Shards!", "§8▸ §eKlicke zum Öffnen!"));
        
        // Info
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§e§l📋 INFO");
        im.setLore(Arrays.asList("§8┃", "§8┃ §7Wähle eine §eKategorie§7!", "§8┃ §7Klicke auf Items zum Kaufen", "§8┃", "§8▸ §7Nutze §e/sell §7zum Verkaufen"));
        info.setItemMeta(im);
        inventory.setItem(49, info);
        
        // Close
        inventory.setItem(45, createButton(Material.BARRIER, "§c§l✖ SCHLIESSEN", "close"));
        
        p.openInventory(inventory);
    }
    
    public void openFoodShop(Player p) {
        inventory = Bukkit.createInventory(this, 54, "§6§l⛃ SCHUL FOOD ⛃");
        fillBorders(inventory);
        
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        ItemStack balInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bi = balInfo.getItemMeta();
        bi.setDisplayName("§6§l⛃ §e§lDEIN GUTHABEN");
        bi.setLore(Arrays.asList("§8┃", "§8┃ §7Balance§8: §a$" + String.format("%.2f", balance)));
        balInfo.setItemMeta(bi);
        inventory.setItem(4, balInfo);
        
        // Items
        for (Map.Entry<Integer, ShopItem> entry : FOOD_ITEMS.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().toItemStack());
        }
        
        // Back
        inventory.setItem(45, createButton(Material.ARROW, "§e§l← ZURÜCK", "back"));
        
        p.openInventory(inventory);
    }
    
    public void openGearShop(Player p) {
        inventory = Bukkit.createInventory(this, 54, "§b§l⛃ SCHUL GEAR ⛃");
        fillBorders(inventory);
        
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        ItemStack balInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bi = balInfo.getItemMeta();
        bi.setDisplayName("§6§l⛃ §e§lDEIN GUTHABEN");
        bi.setLore(Arrays.asList("§8┃", "§8┃ §7Balance§8: §a$" + String.format("%.2f", balance)));
        balInfo.setItemMeta(bi);
        inventory.setItem(4, balInfo);
        
        for (Map.Entry<Integer, ShopItem> entry : GEAR_ITEMS.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().toItemStack());
        }
        
        inventory.setItem(45, createButton(Material.ARROW, "§e§l← ZURÜCK", "back"));
        p.openInventory(inventory);
    }
    
    public void openNetherShop(Player p) {
        inventory = Bukkit.createInventory(this, 54, "§c§l⛃ SCHUL NETHER ⛃");
        fillBorders(inventory);
        
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        ItemStack balInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bi = balInfo.getItemMeta();
        bi.setDisplayName("§6§l⛃ §e§lDEIN GUTHABEN");
        bi.setLore(Arrays.asList("§8┃", "§8┃ §7Balance§8: §a$" + String.format("%.2f", balance)));
        balInfo.setItemMeta(bi);
        inventory.setItem(4, balInfo);
        
        for (Map.Entry<Integer, ShopItem> entry : NETHER_ITEMS.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().toItemStack());
        }
        
        inventory.setItem(45, createButton(Material.ARROW, "§e§l← ZURÜCK", "back"));
        p.openInventory(inventory);
    }
    
    public void openShardShop(Player p) {
        inventory = Bukkit.createInventory(this, 54, "§d§l⛃ SCHUL SHARDS ⛃");
        fillBorders(inventory);
        
        int shards = plugin.getShards().getShards(p.getUniqueId());
        ItemStack shardInfo = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta si = shardInfo.getItemMeta();
        si.setDisplayName("§d§l❖ §5§lDEINE SHARDS");
        si.setLore(Arrays.asList("§8┃", "§8┃ §7Shards§8: §d" + shards));
        shardInfo.setItemMeta(si);
        inventory.setItem(4, shardInfo);
        
        for (Map.Entry<Integer, ShopItem> entry : SHARD_ITEMS.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().toItemStack());
        }
        
        inventory.setItem(45, createButton(Material.ARROW, "§e§l← ZURÜCK", "back"));
        p.openInventory(inventory);
    }
    
    private void fillBorders(Inventory inv) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9) { inv.setItem(i, border); inv.setItem(i + 8, border); }
    }
    
    private ItemStack createButton(Material mat, String name, String action, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(name);
        if (lore.length > 0) m.setLore(Arrays.asList(lore));
        m.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_action"), org.bukkit.persistence.PersistentDataType.STRING, action);
        is.setItemMeta(m);
        return is;
    }
    
    public static ShopItem getShopItem(String title, int slot) {
        if (title.contains("FOOD")) return FOOD_ITEMS.get(slot);
        if (title.contains("GEAR")) return GEAR_ITEMS.get(slot);
        if (title.contains("NETHER")) return NETHER_ITEMS.get(slot);
        if (title.contains("SHARDS")) return SHARD_ITEMS.get(slot);
        return null;
    }
    
    // Shop Item Klasse
    public static class ShopItem {
        public final Material material;
        public final String name;
        public final int price;
        public final int amount;
        public final String spawnerType;
        public final String[] lore;
        public final boolean isShard;
        
        // Normaler Item-Kauf (Food/Gear/Nether)
        public ShopItem(Material material, String name, int price, int amount, String lore1, String lore2) {
            this.material = material;
            this.name = name;
            this.price = price;
            this.amount = amount;
            this.spawnerType = null;
            this.lore = new String[]{lore1, lore2};
            this.isShard = false;
        }
        
        // Shard-Shop Spawner
        public ShopItem(Material material, String name, int price, int amount, String spawnerType, String lore1, String lore2, String lore3) {
            this.material = material;
            this.name = name;
            this.price = price;
            this.amount = amount;
            this.spawnerType = spawnerType;
            this.lore = new String[]{lore1, lore2, lore3};
            this.isShard = true;
        }
        
        public ItemStack toItemStack() {
            ItemStack is = new ItemStack(material, amount);
            ItemMeta m = is.getItemMeta();
            m.setDisplayName(name + " §8x" + amount);
            m.setLore(Arrays.asList(lore));
            is.setItemMeta(m);
            return is;
        }
    }
}
