package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Komplett neuer Donut Shop mit 4 Kategorien: Food, Gear, Nether, End
 */
public class ShopGUI {
    private final DonutPlugin plugin;
    private String currentCategory = "main";
    
    public ShopGUI(DonutPlugin plugin) { 
        this.plugin = plugin; 
    }

    public void open(Player p) {
        openMainMenu(p);
    }
    
    private void openMainMenu(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, toSmallCaps("§6⛃ DONUT SHOP ⛃"));
        GUIUtils.fillBorders(inv, plugin);

        // Spieler-Balance
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        ItemStack balInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bi = balInfo.getItemMeta();
        bi.setDisplayName("§6§l⛃ §e§lDEIN GUTHABEN");
        bi.setLore(Arrays.asList(
            "§8┃",
            "§8┃ §7Balance§8: §a$" + String.format("%.2f", balance),
            "§8┃",
            "§8▸ §7Kaufe Items mit deinem Geld!"
        ));
        balInfo.setItemMeta(bi);
        inv.setItem(4, balInfo);

        // Kategorien
        inv.setItem(20, createCategoryItem(Material.COOKED_BEEF, "§6§l🍖 FOOD", 
            "§8┃ §7Nahrung und Heilung",
            "§8┃ §7Gaps, Steaks & mehr",
            "§8▸ §eKlicke zum Öffnen!"));
            
        inv.setItem(22, createCategoryItem(Material.TOTEM_OF_UNDYING, "§b§l⚔ GEAR", 
            "§8┃ §7Kampf-Ausrüstung",
            "§8┃ §7Totems, Perlen & XP",
            "§8▸ §eKlicke zum Öffnen!"));
            
        inv.setItem(24, createCategoryItem(Material.NETHER_WART, "§c§l✦ NETHER", 
            "§8┃ §7Nether-Ressourcen",
            "§8┃ §7Blazerods, Quarz & mehr",
            "§8▸ §eKlicke zum Öffnen!"));
            
        inv.setItem(30, createCategoryItem(Material.END_STONE, "§5§l⬢ END §8(§c✖§8)", 
            "§8┃ §cGesperrt!",
            "§8┃ §7End-Items später verfügbar",
            "§8▸ §cNoch nicht verfügbar"));
            
        inv.setItem(32, createCategoryItem(Material.SPAWNER, "§d§l❖ SHARD SHOP", 
            "§8┃ §7Spawner & Spezial-Items",
            "§8┃ §7Kaufe mit Shards!",
            "§8▸ §eKlicke zum Öffnen!"));

        // Info
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§e§l📋 INFO");
        im.setLore(Arrays.asList(
            "§8┃",
            "§8┃ §7Wähle eine §eKategorie§7!",
            "§8┃ §7Klicke auf Items zum Kaufen",
            "§8┃",
            "§8▸ §7Nutze §e/sell §7zum Verkaufen"
        ));
        info.setItemMeta(im);
        inv.setItem(49, info);

        // Close Button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        cm.setDisplayName("§c§l✖ SCHLIESSEN");
        close.setItemMeta(cm);
        inv.setItem(45, close);

        p.openInventory(inv);
    }
    
    public void openFoodShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, toSmallCaps("§6⛃ FOOD SHOP ⛃"));
        GUIUtils.fillBorders(inv, plugin);
        
        // Balance
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        inv.setItem(4, createBalanceItem(balance));
        
        // Food Items
        inv.setItem(10, createShopItem(Material.GOLDEN_APPLE, "§6§lGolden Apple §8x1", 50, 1,
            "§8┃ §7Heilung + Absorption",
            "§8▸ §aPreis§8: §e$50"));
            
        inv.setItem(11, createShopItem(Material.COOKED_BEEF, "§c§lSteak §8x16", 80, 16,
            "§8┃ §7Sättigung & Heilung",
            "§8▸ §aPreis§8: §e$80"));
            
        inv.setItem(12, createShopItem(Material.GOLDEN_CARROT, "§6§lGolden Carrot §8x16", 120, 16,
            "§8┃ §7Beste Nahrung!",
            "§8▸ §aPreis§8: §e$120"));
            
        inv.setItem(13, createShopItem(Material.APPLE, "§c§lApple §8x16", 25, 16,
            "§8┃ §7Basis-Nahrung",
            "§8▸ §aPreis§8: §e$25"));
            
        inv.setItem(14, createShopItem(Material.CARROT, "§6§lCarrot §8x16", 20, 16,
            "§8┃ §7Gemüse",
            "§8▸ §aPreis§8: §e$20"));
            
        inv.setItem(15, createShopItem(Material.COOKED_CHICKEN, "§f§lChicken §8x16", 60, 16,
            "§8┃ §7Gebratenes Huhn",
            "§8▸ §aPreis§8: §e$60"));
            
        inv.setItem(16, createShopItem(Material.COOKED_PORKCHOP, "§d§lPork §8x16", 70, 16,
            "§8┃ §7Gebratenes Schwein",
            "§8▸ §aPreis§8: §e$70"));
            
        inv.setItem(19, createShopItem(Material.COOKED_MUTTON, "§7§lMutton §8x16", 55, 16,
            "§8┃ §7Hammelfleisch",
            "§8▸ §aPreis§8: §e$55"));
            
        inv.setItem(20, createShopItem(Material.SWEET_BERRIES, "§c§lSweet Berries §8x32", 30, 32,
            "§8┃ §7Süße Beeren",
            "§8▸ §aPreis§8: §e$30"));
            
        inv.setItem(21, createShopItem(Material.BAKED_POTATO, "§6§lPotato §8x16", 25, 16,
            "§8┃ §7Gebackene Kartoffel",
            "§8▸ §aPreis§8: §e$25"));
            
        inv.setItem(22, createShopItem(Material.MELON_SLICE, "§a§lMelon §8x32", 35, 32,
            "§8┃ §7Melonenscheiben",
            "§8▸ §aPreis§8: §e$35"));
            
        inv.setItem(23, createShopItem(Material.ARROW, "§f§lArrows §8x64", 40, 64,
            "§8┃ §7Munition für Bögen",
            "§8▸ §aPreis§8: §e$40"));
        
        // Back Button
        inv.setItem(45, createBackButton());
        
        p.openInventory(inv);
    }
    
    public void openGearShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, toSmallCaps("§b⛃ GEAR SHOP ⛃"));
        GUIUtils.fillBorders(inv, plugin);
        
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        inv.setItem(4, createBalanceItem(balance));
        
        inv.setItem(11, createShopItem(Material.TOTEM_OF_UNDYING, "§6§lTotem of Undying §8x1", 5000, 1,
            "§8┃ §7Rettet dein Leben!",
            "§8┃ §c§lSELTEN!",
            "§8▸ §aPreis§8: §e$5,000"));
            
        inv.setItem(13, createShopItem(Material.GOLDEN_APPLE, "§6§lGolden Apple §8x8", 380, 8,
            "§8┃ §7Heilung + Absorption",
            "§8▸ §aPreis§8: §e$380"));
            
        inv.setItem(15, createShopItem(Material.EXPERIENCE_BOTTLE, "§a§lXP Bottle §8x64", 250, 64,
            "§8┃ §7Schnelles Leveln",
            "§8▸ §aPreis§8: §e$250"));
            
        inv.setItem(20, createShopItem(Material.ENDER_PEARL, "§b§lEnder Pearl §8x16", 320, 16,
            "§8┃ §7Teleportation",
            "§8▸ §aPreis§8: §e$320"));
            
        inv.setItem(24, createShopItem(Material.ENDER_PEARL, "§b§lEnder Pearl §8x64", 1200, 64,
            "§8┃ §7Viel Teleportation!",
            "§8▸ §aPreis§8: §e$1,200"));
        
        inv.setItem(45, createBackButton());
        
        p.openInventory(inv);
    }
    
    public void openNetherShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, toSmallCaps("§c⛃ NETHER SHOP ⛃"));
        GUIUtils.fillBorders(inv, plugin);
        
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        inv.setItem(4, createBalanceItem(balance));
        
        inv.setItem(10, createShopItem(Material.BLAZE_ROD, "§6§lBlaze Rod §8x8", 180, 8,
            "§8┃ §7Für Tränke & Pulver",
            "§8▸ §aPreis§8: §e$180"));
            
        inv.setItem(11, createShopItem(Material.NETHER_WART, "§c§lNether Wart §8x32", 120, 32,
            "§8┃ §7Basis für Tränke",
            "§8▸ §aPreis§8: §e$120"));
            
        inv.setItem(12, createShopItem(Material.GLOWSTONE, "§e§lGlowstone §8x16", 90, 16,
            "§8┃ §7Leuchtstein",
            "§8▸ §aPreis§8: §e$90"));
            
        inv.setItem(13, createShopItem(Material.MAGMA_CREAM, "§6§lMagma Cream §8x16", 110, 16,
            "§8┃ §7Für Feuerschutz-Tränke",
            "§8▸ §aPreis§8: §e$110"));
            
        inv.setItem(14, createShopItem(Material.QUARTZ, "§f§lQuartz §8x32", 100, 32,
            "§8┃ §7Nether-Quarz",
            "§8▸ §aPreis§8: §e$100"));
            
        inv.setItem(15, createShopItem(Material.SOUL_SAND, "§8§lSoul Sand §8x32", 80, 32,
            "§8┃ §7Seelensand",
            "§8▸ §aPreis§8: §e$80"));
            
        inv.setItem(16, createShopItem(Material.MAGMA_BLOCK, "§6§lMagma Block §8x16", 95, 16,
            "§8┃ §7Magmablock",
            "§8▸ §aPreis§8: §e$95"));
        
        inv.setItem(45, createBackButton());
        
        p.openInventory(inv);
    }
    
    public void openShardShop(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, toSmallCaps("§d⛃ SHARD SHOP ⛃"));
        GUIUtils.fillBorders(inv, plugin);
        
        int shards = plugin.getShards().getShards(p.getUniqueId());
        ItemStack shardInfo = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta si = shardInfo.getItemMeta();
        si.setDisplayName("§d§l❖ §5§lDEINE SHARDS");
        si.setLore(Arrays.asList(
            "§8┃",
            "§8┃ §7Shards§8: §d" + shards,
            "§8┃",
            "§8▸ §7Verdiene Shards durch PvP!"
        ));
        shardInfo.setItemMeta(si);
        inv.setItem(4, shardInfo);
        
        // Spawner
        inv.setItem(11, createShardItem(Material.SPAWNER, "§2§lZombie Spawner §8x1", 500, 1, "ZOMBIE",
            "§8┃ §7Spawnt Zombies",
            "§8┃ §6§lFARMING",
            "§8▸ §aPreis§8: §d500 Shards"));
            
        inv.setItem(12, createShardItem(Material.SPAWNER, "§7§lSkeleton Spawner §8x1", 500, 1, "SKELETON",
            "§8┃ §7Spawnt Skelette",
            "§8┃ §6§lFARMING",
            "§8▸ §aPreis§8: §d500 Shards"));
            
        inv.setItem(13, createShardItem(Material.SPAWNER, "§a§lCreeper Spawner §8x1", 750, 1, "CREEPER",
            "§8┃ §7Spawnt Creeper",
            "§8┃ §6§lFARMING",
            "§8▸ §aPreis§8: §d750 Shards"));
            
        inv.setItem(14, createShardItem(Material.SPAWNER, "§5§lEnderman Spawner §8x1", 1500, 1, "ENDERMAN",
            "§8┃ §7Spawnt Endermen",
            "§8┃ §c§lSELTEN!",
            "§8▸ §aPreis§8: §d1,500 Shards"));
            
        inv.setItem(15, createShardItem(Material.SPAWNER, "§6§lBlaze Spawner §8x1", 1200, 1, "BLAZE",
            "§8┃ §7Spawnt Blazes",
            "§8┃ §c§lSELTEN!",
            "§8▸ §aPreis§8: §d1,200 Shards"));
        
        inv.setItem(45, createBackButton());
        
        p.openInventory(inv);
    }
    
    private ItemStack createCategoryItem(Material mat, String name, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        m.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "shop_category"), 
            org.bukkit.persistence.PersistentDataType.STRING, 
            name);
        is.setItemMeta(m);
        return is;
    }
    
    private ItemStack createShopItem(Material mat, String name, int cost, int amount, String... lore) {
        ItemStack is = new ItemStack(mat, amount);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        m.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "shop_cost_money"), 
            org.bukkit.persistence.PersistentDataType.INTEGER, 
            cost);
        m.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "shop_amount"), 
            org.bukkit.persistence.PersistentDataType.INTEGER, 
            amount);
        is.setItemMeta(m);
        return is;
    }
    
    private ItemStack createShardItem(Material mat, String name, int cost, int amount, String spawnerType, String... lore) {
        ItemStack is = new ItemStack(mat, amount);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        m.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "shop_cost_shards"), 
            org.bukkit.persistence.PersistentDataType.INTEGER, 
            cost);
        m.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "shop_amount"), 
            org.bukkit.persistence.PersistentDataType.INTEGER, 
            amount);
        if (spawnerType != null) {
            m.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "spawner_type"), 
                org.bukkit.persistence.PersistentDataType.STRING, 
                spawnerType);
        }
        is.setItemMeta(m);
        return is;
    }
    
    private ItemStack createBalanceItem(double balance) {
        ItemStack balInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bi = balInfo.getItemMeta();
        bi.setDisplayName("§6§l⛃ §e§lDEIN GUTHABEN");
        bi.setLore(Arrays.asList(
            "§8┃",
            "§8┃ §7Balance§8: §a$" + String.format("%.2f", balance),
            "§8┃"
        ));
        balInfo.setItemMeta(bi);
        return balInfo;
    }
    
    private ItemStack createBackButton() {
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§e§l◄ ZURÜCK");
        bm.setLore(Arrays.asList("§8▸ §7Zurück zum Hauptmenü"));
        bm.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "shop_back"), 
            org.bukkit.persistence.PersistentDataType.STRING, 
            "main");
        back.setItemMeta(bm);
        return back;
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
