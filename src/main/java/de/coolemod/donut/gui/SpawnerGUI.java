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

    public SpawnerGUI(DonutPlugin plugin, PlacedSpawner spawner) {
        this.plugin = plugin;
        this.spawner = spawner;
    }

    public void open(Player player) {
        String rawName = spawner.getType().getDisplayName();
        // Strip color codes for title
        String cleanName = rawName.replaceAll("§.", "");
        String title = "§6⛃ " + cleanName + " ⛃";
        inventory = Bukkit.createInventory(this, 54, title);
        fillBorders();
        updateContent();
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    private void fillBorders() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bm = border.getItemMeta();
        bm.setDisplayName(" ");
        border.setItemMeta(bm);
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }

    private void updateContent() {
        NamespacedKey actionKey = new NamespacedKey(plugin, "spawner_action");

        // === Slot 4: Info Item ===
        ItemStack info = new ItemStack(spawner.getType().getIcon());
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(spawner.getType().getDisplayName());
        im.setLore(Arrays.asList(
            "§8────────────────",
            "§7Anzahl§8: §a" + spawner.getStackSize() + "x",
            "§7Gespeichert§8: " + spawner.getDropsSummary(),
            "§7Cap§8: §e" + spawner.getStoredDropCount() + "§7/§e" + spawner.getMaxStoredDrops(),
            "§8────────────────",
            "§7Sneak+Platzieren = Hand-Stack",
            "§7Sneak+Abbauen = 1 Stack"
        ));
        info.setItemMeta(im);
        inventory.setItem(4, info);

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

        // === Slot 45: Close ===
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§c§l✖ SCHLIESSEN");
        closeMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "close");
        close.setItemMeta(closeMeta);
        inventory.setItem(45, close);

        // === Slot 53: Stack Info ===
        ItemStack stackInfo = new ItemStack(Material.PAPER);
        ItemMeta sm = stackInfo.getItemMeta();
        sm.setDisplayName("§e§l📊 INFO");
        sm.setLore(Arrays.asList("§8────────────────",
            "§7Anzahl§8: §a" + spawner.getStackSize() + "x",
            "§8────────────────",
            "§7Drop-Rate§8: §a" + (plugin.getSpawnerManager().getTicksPerDrop() / 20) + "s",
            "§7Drops pro Tick§8: §a~" + spawner.getStackSize() + " Items"
        ));
        stackInfo.setItemMeta(sm);
        inventory.setItem(53, stackInfo);

        // === Slots 10-43: Stored drops (sorted by material name) ===
        List<ItemStack> drops = spawner.getStoredDrops();
        NamespacedKey dropKey = new NamespacedKey(plugin, "spawner_drop_index");
        int slot = 10;
        for (int i = 0; i < drops.size() && slot < 44; i++) {
            if (slot % 9 == 0 || slot % 9 == 8) { slot++; i--; continue; }
            if (slot >= 45) break;
            ItemStack drop = drops.get(i).clone();
            ItemMeta dm = drop.getItemMeta();
            List<String> lore = dm.hasLore() ? new java.util.ArrayList<>(dm.getLore()) : new java.util.ArrayList<>();
            lore.add("§8────────────────");
            lore.add("§eKlicke zum Einsammeln");
            dm.setLore(lore);
            dm.getPersistentDataContainer().set(dropKey, PersistentDataType.INTEGER, i);
            drop.setItemMeta(dm);
            inventory.setItem(slot, drop);
            slot++;
        }
    }

    public PlacedSpawner getSpawner() { return spawner; }

    @Override
    public Inventory getInventory() { return inventory; }
}
