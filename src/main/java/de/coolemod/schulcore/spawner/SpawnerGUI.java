package de.coolemod.schulcore.spawner;

import de.coolemod.schulcore.SchulCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * GUI für Spawner-Interaktion
 * Zeigt gesammelte Drops und erlaubt Entnahme
 */
public class SpawnerGUI implements InventoryHolder {
    private final SchulCorePlugin plugin;
    private final PlacedSpawner spawner;
    private Inventory inventory;
    
    public SpawnerGUI(SchulCorePlugin plugin, PlacedSpawner spawner) {
        this.plugin = plugin;
        this.spawner = spawner;
    }

    public void open(Player player) {
        String title = toSmallCaps("§6⛃ " + spawner.getType().getDisplayName().substring(2) + " ⛃");
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
        
        // Top row
        for (int i = 0; i < 9; i++) inventory.setItem(i, border);
        // Bottom row
        for (int i = 45; i < 54; i++) inventory.setItem(i, border);
        // Left and right columns
        for (int i = 1; i < 5; i++) {
            inventory.setItem(i * 9, border);
            inventory.setItem(i * 9 + 8, border);
        }
    }

    private void updateContent() {
        // Info-Item
        ItemStack info = new ItemStack(spawner.getType().getIcon());
        ItemMeta im = info.getItemMeta();
        im.setDisplayName(spawner.getType().getDisplayName());
        im.setLore(Arrays.asList(
            "§8────────────────",
            "§7Stack§8: §a" + spawner.getStackSize() + "x",
            "§7Gespeichert§8: " + spawner.getDropsSummary(),
            "§7Cap§8: §e" + spawner.getStoredDropCount() + "§7/§e" + spawner.getMaxStoredDrops(),
            "§8────────────────",
            "§e⚡ Höherer Stack = mehr Cap!",
            "§e⚡ Shift+Rechtsklick zum Stacken"
        ));
        info.setItemMeta(im);
        inventory.setItem(4, info);
        
        // Collect All Button
        ItemStack collectAll = new ItemStack(Material.HOPPER);
        ItemMeta cm = collectAll.getItemMeta();
        cm.setDisplayName("§a§l✓ ALLE EINSAMMELN");
        cm.setLore(Arrays.asList(
            "§8────────────────",
            "§7Sammle alle Items ein!",
            "§8────────────────",
            "§eKlicke zum Einsammeln"
        ));
        cm.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "spawner_action"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "collect_all"
        );
        collectAll.setItemMeta(cm);
        inventory.setItem(48, collectAll);
        
        // Sell All Button
        ItemStack sellAll = new ItemStack(Material.GOLD_INGOT);
        ItemMeta sm2 = sellAll.getItemMeta();
        sm2.setDisplayName("§6§l💰 ALLE VERKAUFEN");
        sm2.setLore(Arrays.asList(
            "§8────────────────",
            "§7Verkaufe alle Drops direkt!",
            "§7Wie §e/sell all§7, aber für Spawner",
            "§8────────────────",
            "§eKlicke zum Verkaufen"
        ));
        sm2.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "spawner_action"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "sell_all"
        );
        sellAll.setItemMeta(sm2);
        inventory.setItem(50, sellAll);
        
        // Close Button
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§c§l✖ SCHLIESSEN");
        closeMeta.getPersistentDataContainer().set(
            new org.bukkit.NamespacedKey(plugin, "spawner_action"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "close"
        );
        close.setItemMeta(closeMeta);
        inventory.setItem(45, close);
        
        // Stack Info
        ItemStack stackInfo = new ItemStack(Material.PAPER);
        ItemMeta sm = stackInfo.getItemMeta();
        sm.setDisplayName("§e§l📊 STACK INFO");
        sm.setLore(Arrays.asList(
            "§8────────────────",
            "§7Aktueller Stack§8: §a" + spawner.getStackSize(),
            "§7Max Stack§8: §e∞",
            "§8────────────────",
            "§7Drop-Rate§8: §a" + (plugin.getSpawnerSystem().getTicksPerDrop() / 20) + "s",
            "§7Drops pro Tick§8: §a~" + spawner.getStackSize() + " Items"
        ));
        stackInfo.setItemMeta(sm);
        inventory.setItem(53, stackInfo);
        
        // Display stored drops
        List<ItemStack> drops = spawner.getStoredDrops();
        int slot = 10;
        for (int i = 0; i < drops.size() && slot < 44; i++) {
            // Skip border slots
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                continue;
            }
            if (slot >= 45) break;
            
            ItemStack drop = drops.get(i).clone();
            ItemMeta dm = drop.getItemMeta();
            List<String> lore = dm.hasLore() ? dm.getLore() : new java.util.ArrayList<>();
            lore.add("§8────────────────");
            lore.add("§eKlicke zum Einsammeln");
            dm.setLore(lore);
            dm.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "spawner_drop_index"),
                org.bukkit.persistence.PersistentDataType.INTEGER,
                i
            );
            drop.setItemMeta(dm);
            inventory.setItem(slot, drop);
            slot++;
        }
    }

    public PlacedSpawner getSpawner() { return spawner; }

    @Override
    public Inventory getInventory() { return inventory; }
    
    private String toSmallCaps(String text) {
        return text.replace("A", "ᴀ").replace("B", "ʙ").replace("C", "ᴄ")
                .replace("D", "ᴅ").replace("E", "ᴇ").replace("F", "ғ")
                .replace("G", "ɢ").replace("H", "ʜ").replace("I", "ɪ")
                .replace("J", "ᴊ").replace("K", "ᴋ").replace("L", "ʟ")
                .replace("M", "ᴍ").replace("N", "ɴ").replace("O", "ᴏ")
                .replace("P", "ᴘ").replace("Q", "ǫ").replace("R", "ʀ")
                .replace("S", "s").replace("T", "ᴛ").replace("U", "ᴜ")
                .replace("V", "ᴠ").replace("W", "ᴡ").replace("X", "x")
                .replace("Y", "ʏ").replace("Z", "ᴢ");
    }
}
