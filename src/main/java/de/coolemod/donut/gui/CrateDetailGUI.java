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

        Inventory inv = GUIUtils.createMenu("§0✦ " + c.display + " §0✦", 6);
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
        infoLore.add("§7Drop: §f1 Ausrüstungsteil deiner Wahl");
        infoLore.add("§7Öffnen: §eMit Auswahl-GUI");
        infoLore.add("§8────────────────");
        cm.setLore(infoLore);
        cm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_crate_id"), org.bukkit.persistence.PersistentDataType.STRING, c.id);
        crateInfo.setItemMeta(cm);
        inv.setItem(4, crateInfo);

        ItemStack previewHeader = new ItemStack(Material.NETHER_STAR);
        ItemMeta previewMeta = previewHeader.getItemMeta();
        previewMeta.setDisplayName("§e§lMögliche Teile");
        List<String> previewLore = new ArrayList<>();
        previewLore.add("§8────────────────");
        previewLore.addAll(plugin.getCrateManager().getTierDescription(c.id));
        previewLore.add("§8────────────────");
        previewMeta.setLore(previewLore);
        previewHeader.setItemMeta(previewMeta);
        inv.setItem(13, previewHeader);

        List<ItemStack> previews = plugin.getCrateManager().getPreviewItems(c.id);
        int[] previewSlots = new int[]{19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < Math.min(previews.size(), previewSlots.length); i++) {
            ItemStack preview = previews.get(i).clone();
            ItemMeta previewItemMeta = preview.getItemMeta();
            if (previewItemMeta != null) {
                List<String> lore = previewItemMeta.hasLore() ? new ArrayList<>(previewItemMeta.getLore()) : new ArrayList<>();
                lore.add(0, "§8────────────────");
                lore.add("§7Dieses Teil kann direkt droppen.");
                previewItemMeta.setLore(lore);
                preview.setItemMeta(previewItemMeta);
            }
            inv.setItem(previewSlots[i], preview);
        }

        ItemStack openInfo = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta openInfoMeta = openInfo.getItemMeta();
        openInfoMeta.setDisplayName("§b§lÖffnungsmodus");
        openInfoMeta.setLore(List.of(
            "§8────────────────",
            "§7Beim Öffnen kannst du",
            "§fdein Gear selbst wählen§7!",
            "§8────────────────"
        ));
        openInfo.setItemMeta(openInfoMeta);
        inv.setItem(31, openInfo);

        ItemStack openBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta obm = openBtn.getItemMeta();
        obm.setDisplayName("§a§l▶ ÖFFNEN & WÄHLEN");
        List<String> obl = new ArrayList<>();
        obl.add("§8────────────────");
        obl.add("§7Keys benötigt: §e1");
        obl.add("§7Deine Keys: " + (keyCount > 0 ? "§a" + keyCount : "§c0"));
        obl.add("§7Reward: §fWähle dein Teil");
        obl.add("§8────────────────");
        obl.add(keyCount > 0 ? "§a✓ Klicke zum Auswählen!" : "§c✗ Du brauchst einen Key!");
        obm.setLore(obl);
        obm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "crate_open:" + c.id);
        openBtn.setItemMeta(obm);
        inv.setItem(48, openBtn);

        int keyCostShards = plugin.getConfig().getInt("settings.key-price-shards." + c.id, 50);
        ItemStack buyBtnShards = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta bsm = buyBtnShards.getItemMeta();
        bsm.setDisplayName("§d§l♦ KEY KAUFEN");
        List<String> bsl = new ArrayList<>();
        bsl.add("§8────────────────");
        bsl.add("§7Preis: §d" + de.coolemod.donut.utils.NumberFormatter.formatInt(keyCostShards) + " Shards");
        bsl.add("§7Deine Shards: §d" + de.coolemod.donut.utils.NumberFormatter.formatInt(plugin.getShards().getShards(p.getUniqueId())));
        bsl.add("§8────────────────");
        bsl.add("§e⬅ Linksklick zum Kaufen!");
        bsm.setLore(bsl);
        bsm.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_gui_action"), org.bukkit.persistence.PersistentDataType.STRING, "crate_buy_shards:" + c.id);
        buyBtnShards.setItemMeta(bsm);
        inv.setItem(49, buyBtnShards);

        // Admin Test Button
        if (p.hasPermission("donut.crate.admin")) {
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
