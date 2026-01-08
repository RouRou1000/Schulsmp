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
 * Verbesserte SlayShop GUI mit mehr Items und schönerem Design
 */
public class SlayShopGUI {
    private final DonutPlugin plugin;
    public SlayShopGUI(DonutPlugin plugin) { this.plugin = plugin; }

    public void open(Player p) {
        Inventory inv = GUIUtils.createMenu("§c⚔ §lSlay Shop §c⚔", 5);
        GUIUtils.fillBorders(inv, plugin);

        // Spieler-Info
        int shards = plugin.getShards().getShards(p.getUniqueId());
        int kills = plugin.getStats().getKills(p.getUniqueId());
        ItemStack info = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§e" + p.getName());
        im.setLore(Arrays.asList("§7Deine Shards: §a" + shards, "§7Deine Kills: §c" + kills));
        info.setItemMeta(im);
        inv.setItem(4, info);

        // Row 2: Combat Items
        inv.setItem(10, createShopItem(Material.GOLDEN_APPLE, "§6Goldener Apfel", 5, Arrays.asList("§7Heilt und gibt Absorption", "§8Kosten: §e5 Shards")));
        inv.setItem(11, createShopItem(Material.ENCHANTED_GOLDEN_APPLE, "§5Verzauberter Goldapfel", 50, Arrays.asList("§7Der ultimative Heilgegenstand", "§8Kosten: §e50 Shards")));
        inv.setItem(12, createShopItem(Material.ENDER_PEARL, "§bEnderperle x8", 10, Arrays.asList("§7Teleportiere dich schnell", "§8Kosten: §e10 Shards")));
        inv.setItem(13, createShopItem(Material.EXPERIENCE_BOTTLE, "§aXP-Flaschen x16", 8, Arrays.asList("§7Schnelles Leveln", "§8Kosten: §e8 Shards")));
        inv.setItem(14, createShopItem(Material.TOTEM_OF_UNDYING, "§6Totem der Unsterblichkeit", 100, Arrays.asList("§7Rettet dein Leben", "§8Kosten: §e100 Shards")));
        inv.setItem(15, createShopItem(Material.ARROW, "§fPfeile x64", 5, Arrays.asList("§7Munition für deinen Bogen", "§8Kosten: §e5 Shards")));
        inv.setItem(16, createShopItem(Material.SPECTRAL_ARROW, "§eSpektralpfeile x32", 10, Arrays.asList("§7Markiert Gegner", "§8Kosten: §e10 Shards")));

        // Row 3: Potions
        inv.setItem(19, createShopItem(Material.SPLASH_POTION, "§cStärketrank II", 15, Arrays.asList("§7+Angriffsstärke", "§8Kosten: §e15 Shards")));
        inv.setItem(20, createShopItem(Material.SPLASH_POTION, "§9Schnelligkeitstrank II", 12, Arrays.asList("§7+Geschwindigkeit", "§8Kosten: §e12 Shards")));
        inv.setItem(21, createShopItem(Material.SPLASH_POTION, "§dRegenerationstrank", 20, Arrays.asList("§7Regeneriert Leben", "§8Kosten: §e20 Shards")));
        inv.setItem(22, createShopItem(Material.SPLASH_POTION, "§7Unsichtbarkeitstrank", 25, Arrays.asList("§7Werde unsichtbar", "§8Kosten: §e25 Shards")));
        inv.setItem(23, createShopItem(Material.LINGERING_POTION, "§4Schadenswolke", 30, Arrays.asList("§7Hinterlässt Schadensfeld", "§8Kosten: §e30 Shards")));
        inv.setItem(24, createShopItem(Material.FIREWORK_ROCKET, "§fFeuerwerk x16", 8, Arrays.asList("§7Zum Fliegen mit Elytra", "§8Kosten: §e8 Shards")));
        inv.setItem(25, createShopItem(Material.OBSIDIAN, "§8Obsidian x16", 15, Arrays.asList("§7Für Bases und Schutz", "§8Kosten: §e15 Shards")));

        // Row 4: Special
        inv.setItem(30, createShopItem(Material.DIAMOND, "§bDiamant x5", 25, Arrays.asList("§7Wertvolle Ressource", "§8Kosten: §e25 Shards")));
        inv.setItem(31, createShopItem(Material.NETHERITE_INGOT, "§4Netherit-Barren", 150, Arrays.asList("§7Das beste Material", "§8Kosten: §e150 Shards")));
        inv.setItem(32, createShopItem(Material.ELYTRA, "§dElytra", 500, Arrays.asList("§7Fliege durch die Lüfte", "§8Kosten: §e500 Shards")));

        GUIUtils.open(p, inv);
    }

    private ItemStack createShopItem(Material mat, String name, int cost, List<String> lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(name);
        m.setLore(lore);
        m.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "shop_cost"), org.bukkit.persistence.PersistentDataType.INTEGER, cost);
        is.setItemMeta(m);
        return is;
    }
}
