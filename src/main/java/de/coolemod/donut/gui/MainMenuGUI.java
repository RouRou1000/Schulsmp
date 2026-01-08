package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Hauptmenü für alle DonutCore Features
 */
public class MainMenuGUI {
    private final DonutPlugin plugin;
    
    public MainMenuGUI(DonutPlugin plugin) {
        this.plugin = plugin;
    }
    
    public void open(Player p) {
        Inventory inv = GUIUtils.createMenu("§6§l✦ §e§lDONUT CORE §6§l✦", 5);
        GUIUtils.fillBorders(inv, plugin);
        
        // Spieler Info
        ItemStack playerInfo = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta pim = playerInfo.getItemMeta();
        pim.setDisplayName("§6§l" + p.getName());
        List<String> pil = new ArrayList<>();
        pil.add("§8────────────────");
        pil.add("§7Geld: §a$" + "%.2f".formatted(plugin.getEconomy().getBalance(p.getUniqueId())));
        pil.add("§7Shards: §d" + plugin.getShards().getShards(p.getUniqueId()));
        pil.add("§7Kills: §c" + plugin.getStats().getKills(p.getUniqueId()));
        pil.add("§7Deaths: §7" + plugin.getStats().getDeaths(p.getUniqueId()));
        pil.add("§8────────────────");
        pim.setLore(pil);
        playerInfo.setItemMeta(pim);
        inv.setItem(4, playerInfo);
        
        // Shop
        ItemStack shop = new ItemStack(Material.EMERALD);
        ItemMeta sm = shop.getItemMeta();
        sm.setDisplayName("§a§l✦ SHOP");
        List<String> sl = new ArrayList<>();
        sl.add("§8────────────────");
        sl.add("§7Kaufe Items mit Geld");
        sl.add("§8────────────────");
        sl.add("§e➤ Klicke zum Öffnen!");
        sm.setLore(sl);
        sm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "open_shop");
        shop.setItemMeta(sm);
        inv.setItem(11, shop);
        
        // Slay Shop
        ItemStack slayShop = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta ssm = slayShop.getItemMeta();
        ssm.setDisplayName("§c§l⚔ SLAY SHOP");
        List<String> ssl = new ArrayList<>();
        ssl.add("§8────────────────");
        ssl.add("§7PvP Items mit Shards");
        ssl.add("§8────────────────");
        ssl.add("§e➤ Klicke zum Öffnen!");
        ssm.setLore(ssl);
        ssm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "open_slayshop");
        slayShop.setItemMeta(ssm);
        inv.setItem(12, slayShop);
        
        // Auktionshaus
        ItemStack ah = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta ahm = ah.getItemMeta();
        ahm.setDisplayName("§6§l✦ AUKTIONSHAUS");
        List<String> ahl = new ArrayList<>();
        ahl.add("§8────────────────");
        ahl.add("§7Kaufe & verkaufe Items");
        ahl.add("§7mit anderen Spielern");
        ahl.add("§8────────────────");
        ahl.add("§e➤ Klicke zum Öffnen!");
        ahm.setLore(ahl);
        ahm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "open_auction");
        ah.setItemMeta(ahm);
        inv.setItem(13, ah);
        
        // Orders
        ItemStack order = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta om = order.getItemMeta();
        om.setDisplayName("§e§l✦ ORDERS");
        List<String> ol = new ArrayList<>();
        ol.add("§8────────────────");
        ol.add("§7Erstelle Kaufanfragen");
        ol.add("§7oder liefere Items");
        ol.add("§8────────────────");
        ol.add("§e➤ Klicke zum Öffnen!");
        om.setLore(ol);
        om.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "open_orders");
        order.setItemMeta(om);
        inv.setItem(14, order);
        
        // Kisten
        ItemStack crates = new ItemStack(Material.ENDER_CHEST);
        ItemMeta cm = crates.getItemMeta();
        cm.setDisplayName("§5§l✦ KISTEN");
        List<String> cl = new ArrayList<>();
        cl.add("§8────────────────");
        cl.add("§7Öffne Belohnungskisten");
        cl.add("§7& kaufe Schlüssel");
        cl.add("§8────────────────");
        cl.add("§e➤ Klicke zum Öffnen!");
        cm.setLore(cl);
        cm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "open_crates");
        crates.setItemMeta(cm);
        inv.setItem(15, crates);
        
        // Sell (Items verkaufen)
        ItemStack sell = new ItemStack(Material.GOLD_INGOT);
        ItemMeta selm = sell.getItemMeta();
        selm.setDisplayName("§a§l$ ITEMS VERKAUFEN");
        List<String> sell_l = new ArrayList<>();
        sell_l.add("§8────────────────");
        sell_l.add("§7Lege Items in die GUI");
        sell_l.add("§7und verkaufe sie für Geld");
        sell_l.add("§8────────────────");
        sell_l.add("§e➤ Klicke um Verkaufs-GUI zu öffnen!");
        selm.setLore(sell_l);
        selm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "open_sell");
        sell.setItemMeta(selm);
        inv.setItem(20, sell);
        
        // Balance
        ItemStack bal = new ItemStack(Material.SUNFLOWER);
        ItemMeta bm = bal.getItemMeta();
        bm.setDisplayName("§e§l💰 KONTOSTAND");
        List<String> bl = new ArrayList<>();
        bl.add("§8────────────────");
        bl.add("§7Dein Geld: §a$" + "%.2f".formatted(plugin.getEconomy().getBalance(p.getUniqueId())));
        bl.add("§7Deine Shards: §d" + plugin.getShards().getShards(p.getUniqueId()));
        bl.add("§8────────────────");
        bm.setLore(bl);
        bal.setItemMeta(bm);
        inv.setItem(22, bal);
        
        // Worth
        ItemStack worth = new ItemStack(Material.SPYGLASS);
        ItemMeta wm = worth.getItemMeta();
        wm.setDisplayName("§e§l🔍 ITEM WERT");
        List<String> wl = new ArrayList<>();
        wl.add("§8────────────────");
        wl.add("§7Zeigt den Wert des Items");
        wl.add("§7in deiner Hand an");
        wl.add("§8────────────────");
        wl.add("§e➤ Klicke um /worth zu nutzen!");
        wm.setLore(wl);
        wm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "run_worth");
        worth.setItemMeta(wm);
        inv.setItem(24, worth);
        
        // Close
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta clm = close.getItemMeta();
        clm.setDisplayName("§c§lSCHLIESSEN");
        clm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "close_menu");
        close.setItemMeta(clm);
        inv.setItem(31, close);
        
        GUIUtils.open(p, inv);
    }
}
