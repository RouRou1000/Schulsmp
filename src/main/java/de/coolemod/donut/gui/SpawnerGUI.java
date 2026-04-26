package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.systems.PlacedSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class SpawnerGUI implements InventoryHolder {
    private final DonutPlugin plugin;
    private final PlacedSpawner spawner;
    private Inventory inventory;
    private int currentPage = 0;

    public SpawnerGUI(DonutPlugin plugin, PlacedSpawner spawner) {
        this.plugin = plugin;
        this.spawner = spawner;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        List<ItemStack> drops = spawner.getStoredDrops();
        int itemsPerPage = 45;
        int totalPages = Math.max(1, (drops.size() + itemsPerPage - 1) / itemsPerPage);
        this.currentPage = Math.max(0, Math.min(page, totalPages - 1));

        String rawName = spawner.getType().getDisplayName();
        String cleanName = rawName.replaceAll("§.", "");
        String title = "§8SPAWNER -> " + cleanName;
        inventory = Bukkit.createInventory(this, 54, title);
        updateContent();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    private void updateContent() {
        NamespacedKey actionKey = new NamespacedKey(plugin, "spawner_action");
        List<ItemStack> drops = spawner.getStoredDrops();
        int itemsPerPage = 45;
        int totalPages = Math.max(1, (drops.size() + itemsPerPage - 1) / itemsPerPage);

        // Fill bottom row (45-53) with neutral panes for a clean consistent layout
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName("§8 ");
        filler.setItemMeta(fillerMeta);
        for (int s = 45; s <= 53; s++) {
            inventory.setItem(s, filler);
        }

        // === Slot 49: Info Item ===
        ItemStack info = new ItemStack(spawner.getType().getIcon());
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(spawner.getType().getDisplayName());
        im.setLore(Arrays.asList(
            "§8────────────────",
            "§7Anzahl§8: §a" + spawner.getStackSize() + "x",
            "§7Gespeichert§8: " + spawner.getDropsSummary(),
            "§7Cap§8: §e" + spawner.getStoredDropCount() + "§7/§e" + spawner.getMaxStoredDrops(),
            "§8────────────────",
            "§7Seite§8: §e" + (currentPage + 1) + "§7/§e" + totalPages
        ));
        im.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "info");
        info.setItemMeta(im);
        inventory.setItem(49, info);

        // === Slot 45: Back Page ===
        if (currentPage > 0) {
            ItemStack back = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
            ItemMeta bm = back.getItemMeta();
            bm.setDisplayName("§e§l« BACK");
            bm.setLore(Arrays.asList("§7Klick: Vorherige Seite"));
            bm.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "prev_page");
            back.setItemMeta(bm);
            inventory.setItem(45, back);
        }

        // === Slot 53: Next Page ===
        if (currentPage < totalPages - 1) {
            ItemStack next = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
            ItemMeta nm = next.getItemMeta();
            nm.setDisplayName("§e§lNEXT »");
            nm.setLore(Arrays.asList("§7Klick: Nächste Seite"));
            nm.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "next_page");
            next.setItemMeta(nm);
            inventory.setItem(53, next);
        }

        // === Slot 48: Collect All ===
        ItemStack collectAll = new ItemStack(Material.HOPPER);
        ItemMeta cm = collectAll.getItemMeta();
        cm.setDisplayName("§a§l✓ ALLE EINSAMMELN");
        cm.setLore(Arrays.asList("§8────────────────", "§7Sammle alle Items ein!", "§8────────────────", "§eKlicke zum Einsammeln"));
        cm.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "collect_all");
        collectAll.setItemMeta(cm);
        inventory.setItem(48, collectAll);

        // === Slot 50: Sell All ===
        ItemStack sellAll = new ItemStack(Material.GOLD_INGOT);
        ItemMeta sm2 = sellAll.getItemMeta();
        sm2.setDisplayName("§6§l💰 ALLE VERKAUFEN");
        sm2.setLore(Arrays.asList("§8────────────────", "§7Verkaufe alle Drops direkt!", "§7Wie §e/sell all§7, aber für Spawner", "§8────────────────", "§eKlicke zum Verkaufen"));
        sm2.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "sell_all");
        sellAll.setItemMeta(sm2);
        inventory.setItem(50, sellAll);

        // === Slot 47: Close ===
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§c§l✖ SCHLIESSEN");
        closeMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "close");
        close.setItemMeta(closeMeta);
        inventory.setItem(47, close);

        // === Slots 0-44: Stored drops on current page ===
        NamespacedKey dropKey = new NamespacedKey(plugin, "spawner_drop_index");
        int start = currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, drops.size());
        for (int globalIndex = start; globalIndex < end; globalIndex++) {
            int slot = globalIndex - start;
            ItemStack drop = drops.get(globalIndex).clone();
            ItemMeta dm = drop.getItemMeta();
            List<String> lore = dm.hasLore() ? new java.util.ArrayList<>(dm.getLore()) : new java.util.ArrayList<>();
            lore.add("§8────────────────");
            lore.add("§eKlicke zum Einsammeln");
            dm.setLore(lore);
            dm.getPersistentDataContainer().set(dropKey, PersistentDataType.INTEGER, globalIndex);
            drop.setItemMeta(dm);
            inventory.setItem(slot, drop);
        }
    }

    public PlacedSpawner getSpawner() { return spawner; }

    public int getCurrentPage() { return currentPage; }

    @Override
    public Inventory getInventory() { return inventory; }
}
