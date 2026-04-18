package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * KOMPLETT NEUER Shop mit InventoryHolder - garantiert sicher!
 */
public class ShopGUI_NEW implements InventoryHolder {
    private final DonutPlugin plugin;
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
        FOOD_ITEMS.put(13, new ShopItem(Material.APPLE, "§c§lApple", 2, 1, "§8┃ §7Basis-Nahrung", "§8▸ §aPreis§8: §e$2"));
        FOOD_ITEMS.put(14, new ShopItem(Material.CARROT, "§6§lCarrot", 1, 1, "§8┃ §7Gemüse", "§8▸ §aPreis§8: §e$1"));
        FOOD_ITEMS.put(15, new ShopItem(Material.COOKED_CHICKEN, "§f§lChicken", 4, 1, "§8┃ §7Gebratenes Huhn", "§8▸ §aPreis§8: §e$4"));
        FOOD_ITEMS.put(16, new ShopItem(Material.COOKED_PORKCHOP, "§d§lPork", 4, 1, "§8┃ §7Gebratenes Schwein", "§8▸ §aPreis§8: §e$4"));
        FOOD_ITEMS.put(19, new ShopItem(Material.COOKED_MUTTON, "§7§lMutton", 4, 1, "§8┃ §7Hammelfleisch", "§8▸ §aPreis§8: §e$4"));
        FOOD_ITEMS.put(20, new ShopItem(Material.SWEET_BERRIES, "§c§lSweet Berries", 1, 1, "§8┃ §7Süße Beeren", "§8▸ §aPreis§8: §e$1"));
        FOOD_ITEMS.put(21, new ShopItem(Material.BAKED_POTATO, "§6§lPotato", 2, 1, "§8┃ §7Gebackene Kartoffel", "§8▸ §aPreis§8: §e$2"));
        FOOD_ITEMS.put(22, new ShopItem(Material.MELON_SLICE, "§a§lMelon", 1, 1, "§8┃ §7Melonenscheiben", "§8▸ §aPreis§8: §e$1"));
        FOOD_ITEMS.put(23, new ShopItem(Material.ARROW, "§f§lArrows", 1, 1, "§8┃ §7Munition für Bögen", "§8▸ §aPreis§8: §e$1"));

        // Gear Shop Items (Preis pro Stück)
        GEAR_ITEMS.put(11, new ShopItem(Material.TOTEM_OF_UNDYING, "§6§lTotem of Undying", 5000, 1, "§8┃ §7Rettet dein Leben! §c§lSELTEN!", "§8▸ §aPreis§8: §e$5,000"));
        GEAR_ITEMS.put(13, new ShopItem(Material.GOLDEN_APPLE, "§6§lGolden Apple", 50, 1, "§8┃ §7Heilung + Absorption", "§8▸ §aPreis§8: §e$50"));
        GEAR_ITEMS.put(15, new ShopItem(Material.EXPERIENCE_BOTTLE, "§a§lXP Bottle", 4, 1, "§8┃ §7Schnelles Leveln", "§8▸ §aPreis§8: §e$4"));
        GEAR_ITEMS.put(20, new ShopItem(Material.ENDER_PEARL, "§b§lEnder Pearl", 20, 1, "§8┃ §7Teleportation", "§8▸ §aPreis§8: §e$20"));

        // Nether Shop Items (Preis pro Stück)
        NETHER_ITEMS.put(10, new ShopItem(Material.BLAZE_ROD, "§6§lBlaze Rod", 6, 1, "§8┃ §7Brauen & Craften", "§8▸ §aPreis§8: §e$6"));
        NETHER_ITEMS.put(12, new ShopItem(Material.NETHER_WART, "§c§lNether Wart", 2, 1, "§8┃ §7Basis für Tränke", "§8▸ §aPreis§8: §e$2"));
        NETHER_ITEMS.put(14, new ShopItem(Material.QUARTZ, "§f§lQuartz", 1, 1, "§8┃ §7Baumaterial", "§8▸ §aPreis§8: §e$1"));
        NETHER_ITEMS.put(16, new ShopItem(Material.MAGMA_CREAM, "§c§lMagma Cream", 5, 1, "§8┃ §7Brauzutat", "§8▸ §aPreis§8: §e$5"));
        NETHER_ITEMS.put(20, new ShopItem(Material.GLOWSTONE_DUST, "§e§lGlowstone Dust", 3, 1, "§8┃ §7Licht & Brauen", "§8▸ §aPreis§8: §e$3"));
        NETHER_ITEMS.put(22, new ShopItem(Material.GHAST_TEAR, "§f§lGhast Tear", 20, 1, "§8┃ §7Regen-Tränke", "§8▸ §aPreis§8: §e$20"));

        // Shard Shop Items
        SHARD_ITEMS.put(31, new ShopItem(Material.AMETHYST_SHARD, "§a§lShard Aufladung", 1000, 1, true,
            "§8┃ §7Kaufe zusaetzliche Shards mit Geld",
            "§8┃ §7Waehle die Menge im Kaufmenue",
            "§8▸ §aPreis§8: §e$1,000 §7pro Shard"));
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
        bi.setLore(Arrays.asList("§8┃", "§8┃ §7Balance§8: §a" + NumberFormatter.formatMoney(balance), "§8┃", "§8▸ §7Kaufe Items mit deinem Geld!"));
        balInfo.setItemMeta(bi);
        inventory.setItem(4, balInfo);

        // Kategorien
        inventory.setItem(20, createButton(Material.COOKED_BEEF, "§6§l🍖 FOOD", "category_food", "§8┃ §7Nahrung und Heilung", "§8┃ §7Gaps, Steaks & mehr", "§8▸ §eKlicke zum Öffnen!"));
        inventory.setItem(22, createButton(Material.TOTEM_OF_UNDYING, "§b§l⚔ GEAR", "category_gear", "§8┃ §7Kampf-Ausrüstung", "§8┃ §7Totems, Perlen & XP", "§8▸ §eKlicke zum Öffnen!"));
        inventory.setItem(24, createButton(Material.NETHER_WART, "§c§l✦ NETHER", "category_nether", "§8┃ §7Nether-Ressourcen", "§8┃ §7Blazerods, Quarz & mehr", "§8▸ §eKlicke zum Öffnen!"));
        inventory.setItem(30, createButton(Material.END_STONE, "§5§l⬢ END §8(§c✖§8)", "category_end", "§8┃ §cGesperrt!", "§8┃ §7End-Items später verfügbar"));
        inventory.setItem(32, createButton(Material.SPAWNER, "§d§l❖ SHARD SHOP", "category_shards", "§8┃ §7Spawner & Shard-Aufladung", "§8┃ §7Spawner kaufen oder Shards aufladen", "§8▸ §eKlicke zum Öffnen!"));

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
        bi.setLore(Arrays.asList("§8┃", "§8┃ §7Balance§8: §a" + NumberFormatter.formatMoney(balance)));
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
        bi.setLore(Arrays.asList("§8┃", "§8┃ §7Balance§8: §a" + NumberFormatter.formatMoney(balance)));
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
        bi.setLore(Arrays.asList("§8┃", "§8┃ §7Balance§8: §a" + NumberFormatter.formatMoney(balance)));
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
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        ItemStack shardInfo = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta si = shardInfo.getItemMeta();
        si.setDisplayName("§d§l❖ §5§lDEINE SHARDS");
        si.setLore(Arrays.asList(
            "§8┃",
            "§8┃ §7Shards§8: §d" + NumberFormatter.formatInt(shards),
            "§8┃ §7Balance§8: §a" + NumberFormatter.formatMoney(balance),
            "§8┃",
            "§8▸ §7Spawner mit Shards kaufen oder Shards aufladen"
        ));
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

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        for (int i = 0; i < 27; i++) inventory.setItem(i, border.clone());

        for (int i = 10; i < 17; i++) inventory.setItem(i, null);

        updateBuyGUI(inventory, session);

        p.openInventory(inventory);
    }

    public void updateBuyGUI(Inventory inv, BuySession session) {
        ShopItem item = session.shopItem;
        double pricePerUnit = (double) item.price / item.amount;
        boolean unlimitedAmount = item.grantsShardBalance;
        int maxStack = unlimitedAmount ? Integer.MAX_VALUE : item.material.getMaxStackSize();
        if (session.amount > maxStack) session.amount = maxStack;
        if (session.amount < 1) session.amount = 1;
        double totalPrice = session.amount * pricePerUnit;

        // Slot 13: Item display
        ItemStack display = new ItemStack(item.material, Math.min(64, Math.max(1, session.amount)));
        ItemMeta dm = display.getItemMeta();
        dm.setDisplayName(item.name);
        List<String> displayLore = new ArrayList<>();
        displayLore.add("§8");
        displayLore.add("§7Menge: §f" + NumberFormatter.formatInt(session.amount) + "x");
        if (item.isShard) {
            displayLore.add("§7Preis/Stück: §d" + NumberFormatter.formatInt((int) Math.ceil(pricePerUnit)) + " Shards");
            displayLore.add("§8");
            displayLore.add("§dGesamtpreis: §5" + NumberFormatter.formatInt((int) Math.ceil(totalPrice)) + " Shards");
        } else if (unlimitedAmount) {
            displayLore.add("§7Preis/Stück: §e" + NumberFormatter.formatMoney(pricePerUnit));
            displayLore.add("§8");
            displayLore.add("§aGesamtpreis: §e" + NumberFormatter.formatMoney(totalPrice));
        } else {
            displayLore.add("§7Preis/Stück: §e" + NumberFormatter.formatMoney(pricePerUnit));
            displayLore.add("§8");
            displayLore.add("§aGesamtpreis: §e" + NumberFormatter.formatMoney(totalPrice));
        }
        dm.setLore(displayLore);
        display.setItemMeta(dm);
        inv.setItem(13, display);

        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);

        // Slot 18: Back
        inv.setItem(18, createButton(Material.RED_STAINED_GLASS_PANE, "§c§l← Zurück", "buy_back"));

        for (int i = 19; i <= 25; i++) inv.setItem(i, border.clone());

        if (unlimitedAmount) {
            // Shard charging: bigger buttons + custom input
            inv.setItem(19, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§l-100", "buy_remove_100", 64));
            inv.setItem(20, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§l-10", "buy_remove_10", 10));
            inv.setItem(21, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§l-1", "buy_remove_1", 1));
            inv.setItem(23, createBuyAmountButton(Material.LIME_STAINED_GLASS_PANE, "§a§l+1", "buy_add_1", 1));
            inv.setItem(24, createBuyAmountButton(Material.LIME_STAINED_GLASS_PANE, "§a§l+10", "buy_add_10", 10));
            inv.setItem(25, createBuyAmountButton(Material.LIME_STAINED_GLASS_PANE, "§a§l+100", "buy_add_100", 64));
            // Custom amount button at slot 10
            ItemStack customBtn = new ItemStack(Material.NAME_TAG);
            ItemMeta cbm = customBtn.getItemMeta();
            cbm.setDisplayName("§e§l✎ EIGENE MENGE");
            cbm.setLore(Arrays.asList("§8────────────────", "§7Gib eine beliebige Menge", "§7im Chat ein!", "§8────────────────", "§eKlicke hier!"));
            cbm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_action"), org.bukkit.persistence.PersistentDataType.STRING, "buy_custom_amount");
            customBtn.setItemMeta(cbm);
            inv.setItem(10, customBtn);
        } else {
            boolean atMax = session.amount >= maxStack;
            if (atMax) {
                inv.setItem(21, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§lRemove 16", "buy_remove_16", 16));
                inv.setItem(22, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§lRemove 1", "buy_remove_1", 1));
            } else {
                inv.setItem(20, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§lRemove 16", "buy_remove_16", 16));
                inv.setItem(21, createBuyAmountButton(Material.RED_STAINED_GLASS_PANE, "§c§lRemove 1", "buy_remove_1", 1));
                inv.setItem(23, createBuyAmountButton(Material.LIME_STAINED_GLASS_PANE, "§a§lAdd 1", "buy_add_1", 1));
                inv.setItem(24, createBuyAmountButton(Material.LIME_STAINED_GLASS_PANE, "§a§lAdd 16", "buy_add_16", 16));
                if (maxStack >= 64) {
                    inv.setItem(25, createBuyAmountButton(Material.LIME_STAINED_GLASS_PANE, "§a§lSet to 64", "buy_set_64", 64));
                }
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
        public final boolean grantsShardBalance;

        private ShopItem(Material material, String name, int price, int amount, String spawnerType,
                         boolean isShard, boolean grantsShardBalance, String... lore) {
            this.material = material;
            this.name = name;
            this.price = price;
            this.amount = amount;
            this.spawnerType = spawnerType;
            this.lore = lore;
            this.isShard = isShard;
            this.grantsShardBalance = grantsShardBalance;
        }

        // Normaler Item-Kauf (Food/Gear/Nether)
        public ShopItem(Material material, String name, int price, int amount, String lore1, String lore2) {
            this(material, name, price, amount, null, false, false, lore1, lore2);
        }

        // Shard-Shop Spawner
        public ShopItem(Material material, String name, int price, int amount, String spawnerType, String lore1, String lore2, String lore3) {
            this(material, name, price, amount, spawnerType, true, false, lore1, lore2, lore3);
        }

        // Geld -> Shard-Guthaben
        public ShopItem(Material material, String name, int price, int amount, boolean grantsShardBalance,
                        String lore1, String lore2, String lore3) {
            this(material, name, price, amount, null, false, grantsShardBalance, lore1, lore2, lore3);
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
