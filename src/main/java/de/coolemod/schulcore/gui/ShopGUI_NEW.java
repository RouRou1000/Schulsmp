package de.coolemod.schulcore.gui;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KOMPLETT NEUER Shop mit InventoryHolder - garantiert sicher!
 */
public class ShopGUI_NEW implements InventoryHolder {
    private final SchulCorePlugin plugin;
    private Inventory inventory;
    private static final Map<UUID, BuySession> BUY_SESSIONS = new ConcurrentHashMap<>();
    
    // Shop Items mit Preis-Mapping
    private static final Map<Integer, ShopItem> MAIN_ITEMS = new HashMap<>();
    private static final Map<Integer, ShopItem> FOOD_ITEMS = new HashMap<>();
    private static final Map<Integer, ShopItem> GEAR_ITEMS = new HashMap<>();
    private static final Map<Integer, ShopItem> NETHER_ITEMS = new HashMap<>();
    private static final Map<Integer, ShopItem> SHARD_ITEMS = new HashMap<>();
    
    static {
        // Food Shop Items (Preis pro Stück)
        FOOD_ITEMS.put(10, new ShopItem(Material.GOLDEN_APPLE, "§6§lGolden Apple", 50, 1, "§8┃ §7Heilung + Absorption", "§8▸ §aPreis§8: §e$50"));
        FOOD_ITEMS.put(11, new ShopItem(Material.COOKED_BEEF, "§c§lSteak", 5, 1, "§8┃ §7Sättigung & Heilung", "§8▸ §aPreis§8: §e$5"));
        FOOD_ITEMS.put(12, new ShopItem(Material.GOLDEN_CARROT, "§6§lGolden Carrot", 8, 1, "§8┃ §7Beste Nahrung!", "§8▸ §aPreis§8: §e$8"));
        FOOD_ITEMS.put(13, new ShopItem(Material.APPLE, "§c§lApple", 12, 1, "§8┃ §7Basis-Nahrung", "§8▸ §aPreis§8: §e$12"));
        FOOD_ITEMS.put(14, new ShopItem(Material.CARROT, "§6§lCarrot", 10, 1, "§8┃ §7Gemüse", "§8▸ §aPreis§8: §e$10"));
        FOOD_ITEMS.put(15, new ShopItem(Material.COOKED_CHICKEN, "§f§lChicken", 4, 1, "§8┃ §7Gebratenes Huhn", "§8▸ §aPreis§8: §e$4"));
        FOOD_ITEMS.put(16, new ShopItem(Material.COOKED_PORKCHOP, "§d§lPork", 4, 1, "§8┃ §7Gebratenes Schwein", "§8▸ §aPreis§8: §e$4"));
        FOOD_ITEMS.put(19, new ShopItem(Material.COOKED_MUTTON, "§7§lMutton", 12, 1, "§8┃ §7Hammelfleisch", "§8▸ §aPreis§8: §e$12"));
        FOOD_ITEMS.put(20, new ShopItem(Material.SWEET_BERRIES, "§c§lSweet Berries", 8, 1, "§8┃ §7Süße Beeren", "§8▸ §aPreis§8: §e$8"));
        FOOD_ITEMS.put(21, new ShopItem(Material.BAKED_POTATO, "§6§lPotato", 2, 1, "§8┃ §7Gebackene Kartoffel", "§8▸ §aPreis§8: §e$2"));
        FOOD_ITEMS.put(22, new ShopItem(Material.MELON_SLICE, "§a§lMelon", 1, 1, "§8┃ §7Melonenscheiben", "§8▸ §aPreis§8: §e$1"));
        FOOD_ITEMS.put(23, new ShopItem(Material.ARROW, "§f§lArrows", 1, 1, "§8┃ §7Munition für Bögen", "§8▸ §aPreis§8: §e$1"));
        
        // Gear Shop Items (Preis pro Stück)
        GEAR_ITEMS.put(13, new ShopItem(Material.GOLDEN_APPLE, "§6§lGolden Apple", 50, 1, "§8┃ §7Heilung + Absorption", "§8▸ §aPreis§8: §e$50"));
        GEAR_ITEMS.put(15, new ShopItem(Material.EXPERIENCE_BOTTLE, "§a§lXP Bottle", 4, 1, "§8┃ §7Schnelles Leveln", "§8▸ §aPreis§8: §e$4"));
        GEAR_ITEMS.put(20, new ShopItem(Material.ENDER_PEARL, "§b§lEnder Pearl", 20, 1, "§8┃ §7Teleportation", "§8▸ §aPreis§8: §e$20"));
        GEAR_ITEMS.put(22, new ShopItem(Material.TOTEM_OF_UNDYING, "§6§lTotem of Undying", 5000, 1, "§8┃ §7Rettet dich vor dem Tod", "§8▸ §aPreis§8: §e$5000"));
        
        // Nether Shop Items (Preis pro Stück)
        NETHER_ITEMS.put(10, new ShopItem(Material.BLAZE_ROD, "§6§lBlaze Rod", 6, 1, "§8┃ §7Brauen & Craften", "§8▸ §aPreis§8: §e$6"));
        NETHER_ITEMS.put(12, new ShopItem(Material.NETHER_WART, "§c§lNether Wart", 15, 1, "§8┃ §7Basis für Tränke", "§8▸ §aPreis§8: §e$15"));
        NETHER_ITEMS.put(14, new ShopItem(Material.QUARTZ, "§f§lQuartz", 1, 1, "§8┃ §7Baumaterial", "§8▸ §aPreis§8: §e$1"));
        NETHER_ITEMS.put(16, new ShopItem(Material.MAGMA_CREAM, "§c§lMagma Cream", 5, 1, "§8┃ §7Brauzutat", "§8▸ §aPreis§8: §e$5"));
        NETHER_ITEMS.put(20, new ShopItem(Material.GLOWSTONE_DUST, "§e§lGlowstone Dust", 8, 1, "§8┃ §7Licht & Brauen", "§8▸ §aPreis§8: §e$8"));
        NETHER_ITEMS.put(22, new ShopItem(Material.GHAST_TEAR, "§f§lGhast Tear", 20, 1, "§8┃ §7Regen-Tränke", "§8▸ §aPreis§8: §e$20"));
        
        // Shard Shop Items - DonutSMP-Style Spawner (erzeugen Drops, keine Mobs!)
        SHARD_ITEMS.put(10, new ShopItem(Material.SPAWNER, "§2§lZombie Spawner", 500, 1, "ZOMBIE", "§8┃ §7Drops§8: §eRotten Flesh, Iron", "§8┃ §a§lBASIS SPAWNER", "§8▸ §aPreis§8: §d500 Shards"));
        SHARD_ITEMS.put(11, new ShopItem(Material.SPAWNER, "§7§lSkeleton Spawner", 500, 1, "SKELETON", "§8┃ §7Drops§8: §eBones, Arrows", "§8┃ §a§lBASIS SPAWNER", "§8▸ §aPreis§8: §d500 Shards"));
        SHARD_ITEMS.put(12, new ShopItem(Material.SPAWNER, "§a§lCreeper Spawner", 750, 1, "CREEPER", "§8┃ §7Drops§8: §eGunpowder", "§8┃ §e§lWERTVOLL", "§8▸ §aPreis§8: §d750 Shards"));
        SHARD_ITEMS.put(13, new ShopItem(Material.SPAWNER, "§8§lSpider Spawner", 600, 1, "SPIDER", "§8┃ §7Drops§8: §eString, Spider Eye", "§8┃ §a§lBASIS SPAWNER", "§8▸ §aPreis§8: §d600 Shards"));
        SHARD_ITEMS.put(14, new ShopItem(Material.SPAWNER, "§f§lCow Spawner", 400, 1, "COW", "§8┃ §7Drops§8: §eBeef, Leather", "§8┃ §a§lFARM SPAWNER", "§8▸ §aPreis§8: §d400 Shards"));
        SHARD_ITEMS.put(15, new ShopItem(Material.SPAWNER, "§d§lPig Spawner", 400, 1, "PIG", "§8┃ §7Drops§8: §ePorkchop", "§8┃ §a§lFARM SPAWNER", "§8▸ §aPreis§8: §d400 Shards"));
        SHARD_ITEMS.put(16, new ShopItem(Material.SPAWNER, "§e§lChicken Spawner", 350, 1, "CHICKEN", "§8┃ §7Drops§8: §eChicken, Feather", "§8┃ §a§lFARM SPAWNER", "§8▸ §aPreis§8: §d350 Shards"));
        SHARD_ITEMS.put(19, new ShopItem(Material.SPAWNER, "§5§lEnderman Spawner", 1500, 1, "ENDERMAN", "§8┃ §7Drops§8: §eEnder Pearls", "§8┃ §c§lSELTEN!", "§8▸ §aPreis§8: §d1500 Shards"));
        SHARD_ITEMS.put(20, new ShopItem(Material.SPAWNER, "§6§lBlaze Spawner", 1200, 1, "BLAZE", "§8┃ §7Drops§8: §eBlaze Rods", "§8┃ §c§lSELTEN!", "§8▸ §aPreis§8: §d1200 Shards"));
        SHARD_ITEMS.put(21, new ShopItem(Material.SPAWNER, "§5§lWitch Spawner", 1000, 1, "WITCH", "§8┃ §7Drops§8: §eGlass, Redstone, etc.", "§8┃ §e§lWERTVOLL", "§8▸ §aPreis§8: §d1000 Shards"));
        SHARD_ITEMS.put(22, new ShopItem(Material.SPAWNER, "§a§lSlime Spawner", 800, 1, "SLIME", "§8┃ §7Drops§8: §eSlime Balls", "§8┃ §e§lWERTVOLL", "§8▸ §aPreis§8: §d800 Shards"));
        SHARD_ITEMS.put(23, new ShopItem(Material.SPAWNER, "§6§lMagma Cube Spawner", 900, 1, "MAGMA_CUBE", "§8┃ §7Drops§8: §eMagma Cream", "§8┃ §e§lWERTVOLL", "§8▸ §aPreis§8: §d900 Shards"));
        SHARD_ITEMS.put(24, new ShopItem(Material.SPAWNER, "§7§l§nIron Golem Spawner", 3000, 1, "IRON_GOLEM", "§8┃ §7Drops§8: §eIron Ingots!", "§8┃ §d§lLEGENDÄR!", "§8▸ §aPreis§8: §d3000 Shards"));
        SHARD_ITEMS.put(25, new ShopItem(Material.SPAWNER, "§0§lWither Skeleton", 2500, 1, "WITHER_SKELETON", "§8┃ §7Drops§8: §eCoal, Skull (selten)", "§8┃ §c§lULTRA SELTEN!", "§8▸ §aPreis§8: §d2500 Shards"));
    }
    
    public ShopGUI_NEW(SchulCorePlugin plugin) {
        this.plugin = plugin;
    }

    // ==================== BUY SESSION ====================
    
    public static class BuySession {
        public ShopItem shopItem;
        public int amount;
        public String shopCategory;
        
        public BuySession(ShopItem item, String category) {
            this.shopItem = item;
            this.amount = 1;
            this.shopCategory = category;
        }
    }
    
    public static void startBuySession(UUID player, ShopItem item, String category) {
        BUY_SESSIONS.put(player, new BuySession(item, category));
    }
    
    public static BuySession getBuySession(UUID player) {
        return BUY_SESSIONS.get(player);
    }
    
    public static void endBuySession(UUID player) {
        BUY_SESSIONS.remove(player);
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

    // ==================== BUY GUI ====================
    
    private String formatMaterialName(Material mat) {
        String name = mat.name().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }
    
    public void openBuyGUI(Player p) {
        BuySession session = BUY_SESSIONS.get(p.getUniqueId());
        if (session == null) return;
        
        ShopItem item = session.shopItem;
        String itemName = formatMaterialName(item.material);
        
        inventory = Bukkit.createInventory(this, 27, "§6§lBUYING " + itemName.toUpperCase());
        
        // Fill all with border
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        for (int i = 0; i < 27; i++) inventory.setItem(i, border.clone());
        
        // Clear center area
        for (int i = 10; i < 17; i++) inventory.setItem(i, null);
        
        // Dynamic content
        updateBuyGUI(inventory, session);
        
        p.openInventory(inventory);
    }
    
    public void updateBuyGUI(Inventory inv, BuySession session) {
        ShopItem item = session.shopItem;
        double pricePerUnit = (double) item.price / item.amount;
        int maxStack = item.material.getMaxStackSize();
        // Clamp amount to max stack
        if (session.amount > maxStack) session.amount = maxStack;
        double totalPrice = session.amount * pricePerUnit;
        boolean atMax = session.amount >= maxStack;
        
        // Slot 13: The item display
        ItemStack display = new ItemStack(item.material, Math.min(maxStack, Math.max(1, session.amount)));
        ItemMeta dm = display.getItemMeta();
        dm.setDisplayName(item.name);
        List<String> displayLore = new ArrayList<>();
        displayLore.add("§8");
        displayLore.add("§7Menge: §f" + session.amount + "x");
        if (item.isShard) {
            displayLore.add("§7Preis/Stück: §d" + NumberFormatter.formatInt((int) Math.ceil(pricePerUnit)) + " Shards");
            displayLore.add("§8");
            displayLore.add("§dGesamtpreis: §5" + NumberFormatter.formatInt((int) Math.ceil(totalPrice)) + " Shards");
        } else {
            displayLore.add("§7Preis/Stück: §e" + NumberFormatter.formatMoney(pricePerUnit));
            displayLore.add("§8");
            displayLore.add("§aGesamtpreis: §e" + NumberFormatter.formatMoney(totalPrice));
        }
        dm.setLore(displayLore);
        display.setItemMeta(dm);
        inv.setItem(13, display);
        
        // Border item for empty slots
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        
        // Slot 18: Back
        inv.setItem(18, createButton(Material.RED_STAINED_GLASS_PANE, "§c§l← Zurück", "buy_back"));
        
        // Clear middle slots first
        for (int i = 19; i <= 25; i++) inv.setItem(i, border.clone());
        
        if (atMax) {
            // At max: only show remove buttons
            inv.setItem(21, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§lRemove 16", "buy_remove_16", 16));
            inv.setItem(22, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§lRemove 1", "buy_remove_1", 1));
        } else {
            // Normal: remove left, add right
            inv.setItem(20, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§lRemove 16", "buy_remove_16", 16));
            inv.setItem(21, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§lRemove 1", "buy_remove_1", 1));
            inv.setItem(23, createBuyAmountButton(Material.LIME_STAINED_GLASS_PANE, "§a§lAdd 1", "buy_add_1", 1));
            inv.setItem(24, createBuyAmountButton(Material.LIME_STAINED_GLASS_PANE, "§a§lAdd 16", "buy_add_16", 16));
            // Only show Set to 64 if material can actually stack to 64
            if (maxStack >= 64) {
                inv.setItem(25, createBuyAmountButton(Material.LIME_STAINED_GLASS_PANE, "§a§lSet to 64", "buy_set_64", 64));
            }
        }
        
        // Slot 26: Confirm
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("§8");
        confirmLore.add("§7Menge: §f" + session.amount + "x");
        if (item.isShard) {
            confirmLore.add("§dPreis: §5" + NumberFormatter.formatInt((int) Math.ceil(totalPrice)) + " Shards");
        } else {
            confirmLore.add("§aPreis: §e" + NumberFormatter.formatMoney(totalPrice));
        }
        confirmLore.add("§8");
        confirmLore.add("§e▸ Klicken zum Kaufen!");
        ItemStack confirm = createButton(Material.LIME_STAINED_GLASS_PANE, "§a§l✓ KAUFEN", "buy_confirm");
        ItemMeta cm = confirm.getItemMeta();
        cm.setLore(confirmLore);
        confirm.setItemMeta(cm);
        inv.setItem(26, confirm);
    }
    
    private ItemStack createBuyAmountButton(Material mat, String name, String action, int stackSize) {
        ItemStack is = new ItemStack(mat, Math.max(1, Math.min(64, stackSize)));
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(name);
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
            ItemStack is = new ItemStack(material, 1);
            ItemMeta m = is.getItemMeta();
            m.setDisplayName(name);
            m.setLore(Arrays.asList(lore));
            is.setItemMeta(m);
            return is;
        }
    }
}
