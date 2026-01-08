package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.AuctionHouseManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginierte Auktionshaus-GUI mit Worth-Anzeige und schönerem Design
 */
public class AuctionGUI {
    private final DonutPlugin plugin;
    public AuctionGUI(DonutPlugin plugin) { this.plugin = plugin; }

    public void open(Player p) { open(p, 1); }

    public void open(Player p, int page) {
        List<AuctionHouseManager.Auction> auctions = new ArrayList<>(plugin.getAuctionManager().getAll());
        int pageSize = 36; // 4 Reihen
        int totalPages = Math.max(1, (auctions.size() + pageSize - 1) / pageSize);
        page = Math.max(1, Math.min(page, totalPages));

        Inventory inv = GUIUtils.createMenu("§x§F§F§A§5§0§0§l⚡ §x§F§F§B§0§2§0§lᴀ§x§F§F§B§B§4§0§lᴜ§x§F§F§C§6§6§0§lᴋ§x§F§F§D§1§8§0§lᴛ§x§F§F§D§C§A§0§lɪ§x§F§F§E§7§C§0§lᴏ§x§F§F§F§2§E§0§lɴ§x§F§F§F§D§F§F§lѕ§x§F§F§F§F§F§F§lʜ§x§F§F§F§F§F§F§lᴀ§x§E§0§F§F§F§F§lᴜ§x§C§0§F§F§F§F§lѕ §8[§7ѕᴇɪᴛᴇ §e" + page + "§8/§e" + totalPages + "§8]", 6);
        GUIUtils.fillBorders(inv, plugin);

        // Header Info mit schönerem Design
        ItemStack info = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§x§F§F§D§7§0§0§l⚡ §x§F§F§E§3§0§0§lᴀ§x§F§F§E§F§0§0§lᴜ§x§F§F§F§B§0§0§lᴋ§x§F§F§F§F§0§7§lᴛ§x§F§F§F§F§1§3§lɪ§x§F§F§F§F§1§F§lᴏ§x§F§F§F§F§2§B§lɴ§x§F§F§F§F§3§7§lѕ§x§F§F§F§F§4§3§lʜ§x§F§F§F§F§4§F§lᴀ§x§F§F§F§F§5§B§lᴜ§x§F§F§F§F§6§7§lѕ");
        List<String> il = new ArrayList<>();
        il.add("§8╔══════════════════════╗");
        il.add("§x§F§F§D§0§0§0 ⚡ §f§lᴀᴋᴛɪᴠᴇ ᴀᴜᴋᴛɪᴏɴᴇɴ: §e§l" + auctions.size());
        il.add("§8");
        il.add("§x§F§F§E§0§0§0 ▸ §7ʟɪɴᴋѕᴋʟɪᴄᴋ ᴢᴜᴍ ᴋᴀᴜꜰᴇɴ");
        il.add("§x§F§F§F§0§0§0 ▸ §7ᴇɪɢᴇɴᴇ ᴀᴜᴋᴛɪᴏɴᴇɴ ᴇʀѕᴛᴇʟʟᴇɴ");
        il.add("§8╚══════════════════════╝");
        im.setLore(il);
        info.setItemMeta(im);
        inv.setItem(4, info);

        int start = (page - 1) * pageSize;
        int slot = 10;
        for (int i = start; i < Math.min(auctions.size(), start + pageSize); i++) {
            if (slot % 9 == 8) slot += 2; // Skip borders
            if (slot >= 44) break;
            AuctionHouseManager.Auction a = auctions.get(i);
            ItemStack is = a.item.clone();
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(0, "§8╔══════════════════════╗");
                lore.add(1, "§x§5§5§F§F§5§5 💰 §7Preis: §x§0§0§F§F§0§0§l$" + String.format("%.2f", a.price));
                double worth = plugin.getWorthManager().getWorth(a.item);
                if (worth > 0) {
                    lore.add(2, "§x§F§F§D§7§0§0 ⚡ §7Item-Wert: §e§l$" + String.format("%.2f", worth));
                    double profit = worth - a.price;
                    if (profit > 0) {
                        lore.add(3, "§x§0§0§F§F§0§0 ✔ §a§lProfit: §l+$" + String.format("%.2f", profit));
                    } else if (profit < 0) {
                        lore.add(3, "§x§F§F§0§0§0§0 ✗ §c§lVerlust: §l-$" + String.format("%.2f", Math.abs(profit)));
                    }
                }
                lore.add("§8");
                lore.add("§x§A§0§A§0§A§0 ▸ §7Verkäufer: §f" + org.bukkit.Bukkit.getOfflinePlayer(a.seller).getName());
                lore.add("§8╚══════════════════════╝");
                lore.add("§x§F§F§E§0§0§0 ▸ §e§lKlicke zum Kaufen");
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "auction_id"), org.bukkit.persistence.PersistentDataType.STRING, a.id);
                meta.setLore(lore);
                is.setItemMeta(meta);
            }
            inv.setItem(slot++, is);
        }

        // Navigation mit moderneren Farben
        if (page > 1) inv.setItem(45, GUIUtils.navItem(plugin, Material.ARROW, "§x§F§F§A§0§0§0§l← §x§F§F§C§0§0§0§lᴠ§x§F§F§E§0§0§0§lᴏ§x§F§F§F§0§0§0§lʀ§x§F§F§F§F§0§0§lʜ§x§F§F§F§F§2§0§lᴇ§x§F§F§F§F§4§0§lʀ§x§F§F§F§F§6§0§lɪ§x§F§F§F§F§8§0§lɢ§x§F§F§F§F§A§0§lᴇ", "auction_prev", page - 1));
        inv.setItem(48, GUIUtils.navItem(plugin, Material.EMERALD, "§x§0§0§F§F§0§0§l+ §x§2§0§F§F§2§0§lᴇ§x§4§0§F§F§4§0§lʀ§x§6§0§F§F§6§0§lѕ§x§8§0§F§F§8§0§lᴛ§x§A§0§F§F§A§0§lᴇ§x§C§0§F§F§C§0§lʟ§x§E§0§F§F§E§0§lʟ§x§F§F§F§F§F§F§lᴇ§x§F§F§F§F§F§F§lɴ", "auction_create", 0));
        inv.setItem(49, GUIUtils.navItem(plugin, Material.BARRIER, "§x§F§F§0§0§0§0§l✗ §x§F§F§2§0§0§0§lѕ§x§F§F§4§0§0§0§lᴄ§x§F§F§6§0§0§0§lʜ§x§F§F§8§0§0§0§lʟ§x§F§F§A§0§0§0§lɪ§x§F§F§C§0§0§0§lᴇ§x§F§F§E§0§0§0§lѕ§x§F§F§F§0§0§0§lѕ§x§F§F§F§F§0§0§lᴇ§x§F§F§F§F§2§0§lɴ", "auction_close", page));
        inv.setItem(50, GUIUtils.navItem(plugin, Material.ENDER_CHEST, "§x§F§F§A§5§0§0§l⚡ §x§F§F§B§7§0§0§lᴍ§x§F§F§C§9§0§0§lᴇ§x§F§F§D§B§0§0§lɪ§x§F§F§E§D§0§0§lɴ§x§F§F§F§F§0§0§lᴇ", "auction_my", 0));
        if (page < totalPages) inv.setItem(53, GUIUtils.navItem(plugin, Material.ARROW, "§x§F§F§F§F§A§0§lɴ§x§F§F§F§F§8§0§lä§x§F§F§F§F§6§0§lᴄ§x§F§F§F§F§4§0§lʜ§x§F§F§F§F§2§0§lѕ§x§F§F§F§F§0§0§lᴛ§x§F§F§F§0§0§0§lᴇ §x§F§F§A§0§0§0§l→", "auction_next", page + 1));

        GUIUtils.open(p, inv);
    }

    public void openMyAuctions(Player p) {
        List<AuctionHouseManager.Auction> myAuctions = plugin.getAuctionManager().getByOwner(p.getUniqueId());
        Inventory inv = GUIUtils.createMenu("§x§F§F§A§5§0§0§l⚡ §x§F§F§B§0§2§0§lᴍ§x§F§F§B§B§4§0§lᴇ§x§F§F§C§6§6§0§lɪ§x§F§F§D§1§8§0§lɴ§x§F§F§D§C§A§0§lᴇ §x§F§F§E§7§C§0§lᴀ§x§F§F§F§2§E§0§lᴜ§x§F§F§F§D§F§F§lᴋ§x§F§F§F§F§F§F§lᴛ§x§F§F§F§F§F§F§lɪ§x§E§0§F§F§F§F§lᴏ§x§C§0§F§F§F§F§lɴ§x§A§0§F§F§F§F§lᴇ§x§8§0§F§F§F§F§lɴ", 6);
        GUIUtils.fillBorders(inv, plugin);

        ItemStack info = new ItemStack(Material.ENDER_CHEST);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§x§F§F§A§5§0§0§l⚡ §x§F§F§C§0§0§0§lᴍ§x§F§F§D§A§0§0§lᴇ§x§F§F§F§4§0§0§lɪ§x§F§F§F§F§0§E§lɴ§x§F§F§F§F§1§C§lᴇ §x§F§F§F§F§2§A§lᴀ§x§F§F§F§F§3§8§lᴜ§x§F§F§F§F§4§6§lᴋ§x§F§F§F§F§5§4§lᴛ§x§F§F§F§F§6§2§lɪ§x§F§F§F§F§7§0§lᴏ§x§F§F§F§F§7§E§lɴ§x§F§F§F§F§8§C§lᴇ§x§F§F§F§F§9§A§lɴ");
        List<String> il = new ArrayList<>();
        il.add("§8╔══════════════════════╗");
        il.add("§x§F§F§D§0§0§0 ⚡ §f§lᴅᴇɪɴᴇ ᴀᴜᴋᴛɪᴏɴᴇɴ: §e§l" + myAuctions.size());
        il.add("§8");
        il.add("§x§F§F§5§0§0§0 ▸ §c§lʀᴇᴄʜᴛѕᴋʟɪᴄᴋ ᴢᴜᴍ ᴢᴜʀüᴄᴋᴢɪᴇʜᴇɴ");
        il.add("§8╚══════════════════════╝");
        im.setLore(il);
        info.setItemMeta(im);
        inv.setItem(4, info);

        int slot = 10;
        for (AuctionHouseManager.Auction a : myAuctions) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
            ItemStack is = a.item.clone();
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add(0, "§8╔══════════════════════╗");
                lore.add(1, "§x§5§5§F§F§5§5 💰 §7Preis: §x§0§0§F§F§0§0§l$" + String.format("%.2f", a.price));
                double worth = plugin.getWorthManager().getWorth(a.item);
                if (worth > 0) {
                    lore.add(2, "§x§F§F§D§7§0§0 ⚡ §7Item-Wert: §e§l$" + String.format("%.2f", worth));
                }
                lore.add("§8");
                lore.add("§8╚══════════════════════╝");
                lore.add("§x§F§F§5§0§0§0 ▸ §c§lRechtsklick zum Zurückziehen");
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "auction_cancel"), org.bukkit.persistence.PersistentDataType.STRING, a.id);
                meta.setLore(lore);
                is.setItemMeta(meta);
            }
            inv.setItem(slot++, is);
        }

        inv.setItem(49, GUIUtils.navItem(plugin, Material.ARROW, "§x§F§F§A§0§0§0§l← §x§F§F§C§0§0§0§lᴢ§x§F§F§E§0§0§0§lᴜ§x§F§F§F§F§0§0§lʀ§x§F§F§F§F§2§0§lü§x§F§F§F§F§4§0§lᴄ§x§F§F§F§F§6§0§lᴋ", "auction_back", 1));
        GUIUtils.open(p, inv);
    }
}
