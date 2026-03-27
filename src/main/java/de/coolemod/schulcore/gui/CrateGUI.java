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
 * Übersicht über alle verfügbaren Kisten mit Tier-Farben und besserer Darstellung
 */
public class CrateGUI {
    private final SchulCorePlugin plugin;
    public CrateGUI(SchulCorePlugin plugin) { this.plugin = plugin; }

    public void open(Player p) {
        List<String> ids = new ArrayList<>(plugin.getCrateManager().getCrateIds());
        int rows = Math.min(6, Math.max(3, (ids.size() + 8) / 9 + 1));
        Inventory inv = GUIUtils.createMenu("§6✦ §lKisten §6✦", rows);
        GUIUtils.fillBorders(inv, plugin);

        int playerShards = plugin.getShards().getShards(p.getUniqueId());

        int slot = 10; // Start nach erstem Rand
        for (String id : ids) {
            if (slot % 9 == 8) slot += 2; // überspringt Ränder
            if (slot >= inv.getSize() - 9) break;
            CrateManager.Crate c = plugin.getCrateManager().getCrate(id);
            Material mat = getMaterialForTier(c.tier);
            ItemStack it = new ItemStack(mat);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(c.display);
            String tier = c.tier == null ? "basic" : c.tier.toLowerCase();
            boolean isHighTier = tier.equals("legendary") || tier.equals("rare");
            List<String> lore = new ArrayList<>();
            lore.add("§8──────────────");
            lore.add("§7Tier: " + getTierColor(c.tier) + (c.tier == null ? "Standard" : c.tier));
            lore.add("§7Enchants: " + (isHighTier ? "§a§lMax" : "§e§lBasis"));
            lore.add("§7Deine Shards: §d" + playerShards);
            lore.add("§8──────────────");
            lore.add("§e➤ Klicke zum Shop");
            m.setLore(lore);
            m.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "donut_crate_id"), org.bukkit.persistence.PersistentDataType.STRING, c.id);
            it.setItemMeta(m);
            inv.setItem(slot++, it);
        }

        // Info-Item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§e§lInfo");
        List<String> il = new ArrayList<>();
        il.add("§7Wähle eine Kategorie");
        il.add("§7und kaufe Rüstung mit §dShards§7!");
        il.add("§7Kein Glück nötig.");
        im.setLore(il);
        info.setItemMeta(im);
        inv.setItem(inv.getSize() - 5, info);

        GUIUtils.open(p, inv);
    }

    private Material getMaterialForTier(String tier) {
        if (tier == null) return Material.CHEST;
        switch (tier.toLowerCase()) {
            case "basic": return Material.CHEST;
            case "rare": return Material.ENDER_CHEST;
            case "legendary": return Material.TRAPPED_CHEST;
            default: return Material.CHEST;
        }
    }

    private String getTierColor(String tier) {
        if (tier == null) return "§7";
        switch (tier.toLowerCase()) {
            case "basic": return "§a";
            case "rare": return "§5";
            case "legendary": return "§6";
            default: return "§7";
        }
    }

    private int countKeys(Player p, String crateId) {
        int cnt = 0;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is == null) continue;
            if (is.getType() == Material.TRIPWIRE_HOOK && is.hasItemMeta()) {
                String id = is.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "donut_crate_id"), org.bukkit.persistence.PersistentDataType.STRING);
                if (crateId.equals(id)) cnt += is.getAmount();
            }
        }
        return cnt;
    }
}