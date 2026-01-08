package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Erweiterter Donut Shop mit Economy-Items, Spawner und mehr
 */
public class ShopGUI {
    private final DonutPlugin plugin;
    public ShopGUI(DonutPlugin plugin) { this.plugin = plugin; }

    public void open(Player p) {
        Inventory inv = GUIUtils.createMenu("§6✧ §lDonut Shop §6✧", 6);
        GUIUtils.fillBorders(inv, plugin);

        // Spieler-Balance anzeigen
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        ItemStack balInfo = new ItemStack(Material.GOLD_INGOT);
        ItemMeta bi = balInfo.getItemMeta();
        bi.setDisplayName("§eDein Guthaben");
        bi.setLore(Arrays.asList("§7Balance: §a$" + String.format("%.2f", balance)));
        balInfo.setItemMeta(bi);
        inv.setItem(4, balInfo);

        // Kategorie: Ressourcen
        inv.setItem(10, createItem(Material.COBBLESTONE, "§7Cobblestone x64", 10, "§8Kaufen für §a$10"));
        inv.setItem(11, createItem(Material.OAK_LOG, "§6Eichenholz x32", 25, "§8Kaufen für §a$25"));
        inv.setItem(12, createItem(Material.IRON_INGOT, "§fEisenbarren x16", 80, "§8Kaufen für §a$80"));
        inv.setItem(13, createItem(Material.GOLD_INGOT, "§6Goldbarren x8", 100, "§8Kaufen für §a$100"));
        inv.setItem(14, createItem(Material.DIAMOND, "§bDiamant x4", 500, "§8Kaufen für §a$500"));
        inv.setItem(15, createItem(Material.EMERALD, "§aSmaragd x8", 300, "§8Kaufen für §a$300"));
        inv.setItem(16, createItem(Material.NETHERITE_SCRAP, "§4Netherit-Schrott", 1000, "§8Kaufen für §a$1000"));

        // Kategorie: Nahrung
        inv.setItem(19, createItem(Material.BREAD, "§6Brot x32", 15, "§8Kaufen für §a$15"));
        inv.setItem(20, createItem(Material.COOKED_BEEF, "§cSteak x16", 30, "§8Kaufen für §a$30"));
        inv.setItem(21, createItem(Material.GOLDEN_CARROT, "§6Goldene Karotten x8", 50, "§8Kaufen für §a$50"));
        inv.setItem(22, createItem(Material.CAKE, "§dKuchen", 100, "§8Kaufen für §a$100"));

        // Kategorie: Werkzeuge
        inv.setItem(24, createItem(Material.DIAMOND_PICKAXE, "§bDiamant-Spitzhacke", 800, "§8Kaufen für §a$800"));
        inv.setItem(25, createItem(Material.DIAMOND_SWORD, "§bDiamant-Schwert", 600, "§8Kaufen für §a$600"));

        // Kategorie: Spawner
        inv.setItem(28, createItem(Material.SPAWNER, "§cZombie-Spawner", 5000, "§8Kaufen für §a$5000"));
        inv.setItem(29, createItem(Material.SPAWNER, "§7Skelett-Spawner", 5000, "§8Kaufen für §a$5000"));
        inv.setItem(30, createItem(Material.SPAWNER, "§2Creeper-Spawner", 8000, "§8Kaufen für §a$8000"));
        inv.setItem(31, createItem(Material.SPAWNER, "§5Enderman-Spawner", 15000, "§8Kaufen für §a$15000"));
        inv.setItem(32, createItem(Material.SPAWNER, "§6Blaze-Spawner", 12000, "§8Kaufen für §a$12000"));

        // Kategorie: Spezial
        inv.setItem(37, createItem(Material.NAME_TAG, "§eNametag", 200, "§8Kaufen für §a$200"));
        inv.setItem(38, createItem(Material.SADDLE, "§6Sattel", 150, "§8Kaufen für §a$150"));
        inv.setItem(39, createItem(Material.LEAD, "§aLeine x4", 50, "§8Kaufen für §a$50"));
        inv.setItem(40, createItem(Material.ENCHANTED_BOOK, "§dVerzaubertes Buch (Zufall)", 1500, "§8Kaufen für §a$1500"));

        // Navigation
        inv.setItem(45, GUIUtils.navItem(plugin, Material.BARRIER, "§cSchließen", "shop_close", 0));
        inv.setItem(49, createItem(Material.PAPER, "§eInfo", 0, "§7Klicke auf Items zum Kaufen"));
        inv.setItem(53, GUIUtils.navItem(plugin, Material.CHEST, "§6Auktionshaus", "open_auction", 0));

        GUIUtils.open(p, inv);
    }

    private ItemStack createItem(Material mat, String name, int cost, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        if (cost > 0) m.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_cost_money"), org.bukkit.persistence.PersistentDataType.INTEGER, cost);
        is.setItemMeta(m);
        return is;
    }
}
