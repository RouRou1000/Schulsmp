package de.coolemod.schulcore.gui;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.managers.CrateManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Rüstungs-Shop: Spieler wählen ein einzelnes Teil und kaufen es direkt mit Shards.
 * Kein Glück, keine Keys, keine Animation – einfach auswählen und kaufen.
 */
public class CrateDetailGUI {
    private final SchulCorePlugin plugin;
    public CrateDetailGUI(SchulCorePlugin plugin) { this.plugin = plugin; }

    public void open(Player p, String crateId) {
        CrateManager.Crate c = plugin.getCrateManager().getCrate(crateId);
        if (c == null) { p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Nicht gefunden."); return; }
        
        String tier = c.tier == null ? "basic" : c.tier.toLowerCase();
        boolean isHighTier = tier.equals("legendary") || tier.equals("rare");
        
        // Tier styling
        String tierColor;
        Material tierMat;
        switch (tier) {
            case "legendary": tierColor = "§6"; tierMat = Material.NETHERITE_CHESTPLATE; break;
            case "rare":      tierColor = "§b"; tierMat = Material.DIAMOND_CHESTPLATE; break;
            default:           tierColor = "§a"; tierMat = Material.DIAMOND_CHESTPLATE; break;
        }
        
        int playerShards = plugin.getShards().getShards(p.getUniqueId());

        Inventory inv = GUIUtils.createMenu(tierColor + "§l✦ " + c.display + " §8- Shop", 5);
        GUIUtils.fillBorders(inv, plugin);

        // Info oben mittig
        ItemStack info = new ItemStack(tierMat);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(tierColor + "✦ " + c.display + " ✦");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8────────────────");
        infoLore.add("§7Wähle ein Rüstungsteil");
        infoLore.add("§7und kaufe es mit §dShards§7!");
        infoLore.add("§8");
        infoLore.add("§7Deine Shards: §d" + playerShards);
        infoLore.add("§8────────────────");
        im.setLore(infoLore);
        info.setItemMeta(im);
        inv.setItem(4, info);

        // Preise laden
        int helmetPrice  = plugin.getConfig().getInt("settings.armor-prices." + tier + ".helmet", 100);
        int chestPrice    = plugin.getConfig().getInt("settings.armor-prices." + tier + ".chestplate", 150);
        int legsPrice     = plugin.getConfig().getInt("settings.armor-prices." + tier + ".leggings", 130);
        int bootsPrice    = plugin.getConfig().getInt("settings.armor-prices." + tier + ".boots", 80);

        // Slot 11: Helmet
        inv.setItem(11, createBuyItem(
            getArmorMaterial(tier, "HELMET"), tierColor + "§l⛑ Helm",
            getEnchantLore("helmet", isHighTier), helmetPrice, playerShards,
            "armor_buy:" + tier + ":HELMET"
        ));

        // Slot 13: Chestplate
        inv.setItem(13, createBuyItem(
            getArmorMaterial(tier, "CHESTPLATE"), tierColor + "§l🛡 Brustplatte",
            getEnchantLore("chestplate", isHighTier), chestPrice, playerShards,
            "armor_buy:" + tier + ":CHESTPLATE"
        ));

        // Slot 15: Leggings
        inv.setItem(15, createBuyItem(
            getArmorMaterial(tier, "LEGGINGS"), tierColor + "§l👖 Hose",
            getEnchantLore("leggings", isHighTier), legsPrice, playerShards,
            "armor_buy:" + tier + ":LEGGINGS"
        ));

        // Slot 17 → eigentlich Rand, also Slot 20
        // Besser: Reihe 3 Slot 20: Boots
        inv.setItem(20, createBuyItem(
            getArmorMaterial(tier, "BOOTS"), tierColor + "§l👢 Stiefel",
            getEnchantLore("boots", isHighTier), bootsPrice, playerShards,
            "armor_buy:" + tier + ":BOOTS"
        ));

        // Komplettes Set kaufen
        int fullSetPrice = helmetPrice + chestPrice + legsPrice + bootsPrice;
        int discountedPrice = (int)(fullSetPrice * 0.85); // 15% Rabatt
        ItemStack setBtn = new ItemStack(Material.NETHER_STAR);
        ItemMeta sm = setBtn.getItemMeta();
        sm.setDisplayName(tierColor + "§l★ KOMPLETTES SET");
        List<String> setLore = new ArrayList<>();
        setLore.add("§8────────────────");
        setLore.add("§7Helm + Brustplatte + Hose + Stiefel");
        setLore.add("§8");
        setLore.addAll(getEnchantLore("all", isHighTier));
        setLore.add("§8");
        setLore.add("§7Einzelpreis: §8§m" + fullSetPrice + " Shards");
        setLore.add("§7Set-Preis: §d§l" + discountedPrice + " Shards §7(§a-15%§7)");
        setLore.add("§8────────────────");
        setLore.add(playerShards >= discountedPrice ? "§a✓ Klicke zum Kaufen!" : "§c✗ Nicht genug Shards!");
        sm.setLore(setLore);
        sm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"),
            org.bukkit.persistence.PersistentDataType.STRING, "armor_buy_set:" + tier);
        setBtn.setItemMeta(sm);
        inv.setItem(22, setBtn);

        // Shards-Info unten
        ItemStack shardsInfo = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta sim = shardsInfo.getItemMeta();
        sim.setDisplayName("§d§l♦ Deine Shards");
        List<String> sl = new ArrayList<>();
        sl.add("§8────────────────");
        sl.add("§7Shards: §d" + playerShards);
        sl.add("§8────────────────");
        sim.setLore(sl);
        shardsInfo.setItemMeta(sim);
        inv.setItem(40, shardsInfo);

        // Zurück-Button
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backmeta = backBtn.getItemMeta();
        backmeta.setDisplayName("§c← Zurück");
        backmeta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"),
            org.bukkit.persistence.PersistentDataType.STRING, "crate_back");
        backBtn.setItemMeta(backmeta);
        inv.setItem(36, backBtn);

        GUIUtils.open(p, inv);
    }

    private ItemStack createBuyItem(Material mat, String name, List<String> enchants, int price, int playerShards, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────");
        lore.addAll(enchants);
        lore.add("§8");
        lore.add("§7Preis: §d" + price + " Shards");
        lore.add("§8────────────────");
        lore.add(playerShards >= price ? "§a✓ Klicke zum Kaufen!" : "§c✗ Nicht genug Shards!");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"),
            org.bukkit.persistence.PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> getEnchantLore(String piece, boolean isHighTier) {
        List<String> lore = new ArrayList<>();
        lore.add("§7Protection " + (isHighTier ? "IV" : "II"));
        lore.add("§7Unbreaking " + (isHighTier ? "III" : "I"));
        if (isHighTier) lore.add("§7Mending");
        if (piece.equals("helmet") || piece.equals("all")) {
            if (isHighTier) {
                lore.add("§7Aqua Affinity");
                lore.add("§7Respiration III");
            }
        }
        if (piece.equals("boots") || piece.equals("all")) {
            if (isHighTier) {
                lore.add("§7Feather Falling IV");
                lore.add("§7Depth Strider III");
            }
        }
        return lore;
    }

    private Material getArmorMaterial(String tier, String piece) {
        if (tier == null) tier = "basic";
        String prefix;
        switch (tier.toLowerCase()) {
            case "legendary": prefix = "NETHERITE_"; break;
            case "rare": prefix = "DIAMOND_"; break;
            default: prefix = "DIAMOND_"; break;
        }
        try {
            return Material.valueOf(prefix + piece);
        } catch (IllegalArgumentException e) {
            return Material.DIAMOND_CHESTPLATE;
        }
    }
}