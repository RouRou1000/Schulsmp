package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.CrateManager;
import de.coolemod.donut.utils.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Detailansicht einer Kiste: Vorschau, garantierte Items, Open/Buy-Buttons
 */
public class CrateDetailGUI {
    private final DonutPlugin plugin;
    public CrateDetailGUI(DonutPlugin plugin) { this.plugin = plugin; }

    public void open(Player p, String crateId) {
        CrateManager.Crate c = plugin.getCrateManager().getCrate(crateId);
        if (c == null) { p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Kiste nicht gefunden."); return; }
        
        // Größeres GUI für bessere Übersicht
        Inventory inv = GUIUtils.createMenu("§5✦ Kiste: §d" + c.display + " §5✦", 6);
        GUIUtils.fillBorders(inv, plugin);
        
        // Tier-Material basierend auf Tier
        Material tierMat = Material.CHEST;
        String tierColor = "§7";
        if (c.tier != null) {
            switch (c.tier.toLowerCase()) {
                case "legendary": tierMat = Material.ENDER_CHEST; tierColor = "§6"; break;
                case "rare": tierMat = Material.TRAPPED_CHEST; tierColor = "§b"; break;
                default: tierMat = Material.CHEST; tierColor = "§a"; break;
            }
        }

        // Zeige Kisten-Info oben mittig
        ItemStack crateInfo = new ItemStack(tierMat);
        ItemMeta cm = crateInfo.getItemMeta();
        cm.setDisplayName(tierColor + "✦ " + c.display + " ✦");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8────────────────");
        infoLore.add("§7ID: §e" + c.id);
        infoLore.add("§7Tier: " + tierColor + (c.tier == null ? "Standard" : c.tier));
        infoLore.add("§7Schlüssel: §f" + c.keyName);
        int keyCount = plugin.getCrateManager().getKeyCount(p.getUniqueId(), c.id);
        infoLore.add("§7Deine Keys: " + (keyCount > 0 ? "§a" + keyCount : "§c0"));
        double enchChance = c.tier == null ? 0.0 : plugin.getConfig().getDouble("settings.crate-tiers." + c.tier + ".enchant-chance", 0.0);
        infoLore.add("§7Verzauberungschance: §d" + (int)(enchChance * 100) + "%");
        infoLore.add("§8────────────────");
        cm.setLore(infoLore);
        cm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_crate_id"), org.bukkit.persistence.PersistentDataType.STRING, c.id);
        crateInfo.setItemMeta(cm);
        inv.setItem(4, crateInfo);

        // Garantierte Items Header
        ItemStack gHead = new ItemStack(Material.NETHER_STAR);
        ItemMeta ghm = gHead.getItemMeta();
        ghm.setDisplayName("§a✓ Garantierte Items");
        List<String> ghLore = new ArrayList<>();
        ghLore.add("§7Du erhältst diese Items");
        ghLore.add("§7bei jedem Öffnen!");
        ghm.setLore(ghLore);
        gHead.setItemMeta(ghm);
        inv.setItem(9, gHead);

        // Garantierte Artikel (Zeile 2, Slots 10-17)
        int gslot = 10;
        for (int i = 0; i < c.guaranteed.size() && gslot < 17; i++) {
            ItemStack g = c.guaranteed.get(i).item.clone();
            ItemMeta gm = g.getItemMeta();
            if (gm != null) {
                List<String> lore = gm.hasLore() ? new ArrayList<>(gm.getLore()) : new ArrayList<>();
                lore.addFirst("§a✓ §7GARANTIERT");
                lore.add(1, "§8────────────────");
                gm.setLore(lore);
                g.setItemMeta(gm);
            }
            inv.setItem(gslot++, g);
        }
        if (c.guaranteed.isEmpty()) {
            ItemStack noG = new ItemStack(Material.BARRIER);
            ItemMeta ngm = noG.getItemMeta();
            ngm.setDisplayName("§7Keine garantierten Items");
            noG.setItemMeta(ngm);
            inv.setItem(10, noG);
        }

        // Pool Header
        ItemStack pHead = new ItemStack(Material.CHEST_MINECART);
        ItemMeta phm = pHead.getItemMeta();
        phm.setDisplayName("§e♦ Mögliche Belohnungen");
        List<String> phLore = new ArrayList<>();
        phLore.add("§7Gewichtet zufällige Items");
        phLore.add("§7aus dem Pool.");
        phm.setLore(phLore);
        pHead.setItemMeta(phm);
        inv.setItem(18, pHead);

        // Pool Items (Zeile 3-4, Slots 19-26 und 28-35)
        int pslot = 19;
        int poolCount = 0;
        for (int i = 0; i < c.pool.size() && poolCount < 14; i++) {
            CrateManager.PoolEntry pe = c.pool.get(i);
            ItemStack it = pe.item.clone();
            ItemMeta itm = it.getItemMeta();
            if (itm != null) {
                List<String> lore = itm.hasLore() ? new ArrayList<>(itm.getLore()) : new ArrayList<>();
                lore.addFirst("§e♦ §7Chance: §e" + pe.weight);
                lore.add(1, "§8────────────────");
                itm.setLore(lore);
                it.setItemMeta(itm);
            }
            // Skip borders
            if (pslot == 27) pslot = 28;
            if (pslot == 36) break;
            inv.setItem(pslot++, it);
            poolCount++;
        }

        // Bundle-Vorschau (falls vorhanden)
        if (!c.bundles.isEmpty()) {
            ItemStack bundleInfo = new ItemStack(Material.BUNDLE);
            ItemMeta bim = bundleInfo.getItemMeta();
            bim.setDisplayName("§b✿ Bundle-Belohnungen");
            List<String> blore = new ArrayList<>();
            blore.add("§8────────────────");
            blore.add("§7Mögliche Bündel:");
            int shown = 0;
            for (List<ItemStack> bundle : c.bundles) {
                if (shown >= 3) { blore.add("§7... und mehr"); break; }
                StringBuilder sb = new StringBuilder("§e• ");
                for (ItemStack bi : bundle) sb.append(bi.getAmount()).append("x ").append(bi.getType().name().toLowerCase().replace("_", " ")).append(", ");
                if (sb.length() > 4) blore.add(sb.substring(0, sb.length() - 2));
                shown++;
            }
            blore.add("§8────────────────");
            bim.setLore(blore);
            bundleInfo.setItemMeta(bim);
            inv.setItem(17, bundleInfo);
        }

        // Untere Buttons
        // Öffnen-Button
        ItemStack openBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta obm = openBtn.getItemMeta();
        obm.setDisplayName("§a§l▶ ÖFFNEN");
        List<String> obl = new ArrayList<>();
        obl.add("§8────────────────");
        obl.add("§7Keys benötigt: §e1");
        obl.add("§7Deine Keys: " + (keyCount > 0 ? "§a" + keyCount : "§c0"));
        obl.add("§8────────────────");
        obl.add(keyCount > 0 ? "§a✓ Klicke zum Öffnen!" : "§c✗ Du brauchst einen Key!");
        obm.setLore(obl);
        obm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "crate_open:" + c.id);
        openBtn.setItemMeta(obm);
        inv.setItem(48, openBtn);
        
        // Kaufen-Button (NUR SHARDS - Geld entfernt!)
        int keyCostShards = plugin.getConfig().getInt("settings.key-price-shards." + c.id, 50);
        ItemStack buyBtnShards = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta bsm = buyBtnShards.getItemMeta();
        bsm.setDisplayName("§d§l♦ KEY KAUFEN");
        List<String> bsl = new ArrayList<>();
        bsl.add("§8────────────────");
        bsl.add("§7Preis: §d" + keyCostShards + " Shards");
        bsl.add("§7Deine Shards: §d" + plugin.getShards().getShards(p.getUniqueId()));
        bsl.add("§8────────────────");
        bsl.add("§e⬅ Linksklick zum Kaufen!");
        bsm.setLore(bsl);
        bsm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "crate_buy_shards:" + c.id);
        buyBtnShards.setItemMeta(bsm);
        inv.setItem(49, buyBtnShards);

        // Admin Test Button
        if (p.hasPermission("donut.admin")) {
            ItemStack testBtn = new ItemStack(Material.COMMAND_BLOCK);
            ItemMeta tbm = testBtn.getItemMeta();
            tbm.setDisplayName("§c§l⚙ ADMIN TEST");
            List<String> tbl = new ArrayList<>();
            tbl.add("§8────────────────");
            tbl.add("§7Öffnet die Kiste");
            tbl.add("§7ohne Key zu verbrauchen.");
            tbl.add("§8────────────────");
            tbl.add("§c⚠ Nur für Admins!");
            tbm.setLore(tbl);
            tbm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "crate_test:" + c.id);
            testBtn.setItemMeta(tbm);
            inv.setItem(50, testBtn);
        }

        // Zurück-Button
        ItemStack backBtn = new ItemStack(Material.ARROW);
        ItemMeta backmeta = backBtn.getItemMeta();
        backmeta.setDisplayName("§c← Zurück");
        backmeta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "crate_back");
        backBtn.setItemMeta(backmeta);
        inv.setItem(45, backBtn);

        GUIUtils.open(p, inv);
    }
}