package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.SpawnerGUI;
import de.coolemod.donut.managers.SpawnerManager;
import de.coolemod.donut.systems.PlacedSpawner;
import de.coolemod.donut.systems.SpawnerType;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;

public class SpawnerBreakListener implements Listener {
    private final DonutPlugin plugin;
    private final SpawnerManager manager;

    public SpawnerBreakListener(DonutPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getSpawnerManager();
    }

    // === PLACE SPAWNER ===
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item.getType() != Material.SPAWNER) return;
        SpawnerType type = manager.getSpawnerType(item);
        if (type == null) return;
        Location loc = e.getBlock().getLocation();
        Player p = e.getPlayer();

        int logicalStackPerItem = Math.max(1, manager.getSpawnerStack(item));
        int consumedItems = p.isSneaking() ? item.getAmount() : 1;
        int placeAmount = consumedItems * logicalStackPerItem;

        if (manager.placeSpawner(loc, type, placeAmount, p.getUniqueId())) {
            if (consumedItems > 1) {
                int remainingItems = Math.max(0, item.getAmount() - consumedItems);
                EquipmentSlot handSlot = e.getHand();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (!p.isOnline()) {
                        return;
                    }
                    if (handSlot == EquipmentSlot.OFF_HAND) {
                        p.getInventory().setItemInOffHand(remainingItems > 0 ? manager.createSpawnerItem(type, logicalStackPerItem) : null);
                        if (remainingItems > 0) {
                            p.getInventory().getItemInOffHand().setAmount(remainingItems);
                        }
                    } else {
                        p.getInventory().setItemInMainHand(remainingItems > 0 ? manager.createSpawnerItem(type, logicalStackPerItem) : null);
                        if (remainingItems > 0) {
                            p.getInventory().getItemInMainHand().setAmount(remainingItems);
                        }
                    }
                    p.updateInventory();
                });
            }
            p.sendMessage("");
            p.sendMessage("§8┃ §6§l⚡ SPAWNER §8┃ §a" + (placeAmount > 1 ? placeAmount + "x " : "") + type.getDisplayName() + " §7platziert!");
            p.sendMessage("§8┃ §7Rechtsklick zum Sammeln der Drops");
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);
        } else {
            e.setCancelled(true);
            p.sendMessage("§8┃ §c§l✖ FEHLER §8┃ §cSpawner konnte nicht platziert werden!");
        }
    }

    // === BREAK SPAWNER ===
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        Block block = e.getBlock();
        if (block.getType() != Material.SPAWNER) return;
        Location loc = block.getLocation();
        PlacedSpawner ps = manager.getPlacedSpawner(loc);
        if (ps == null) return;
        Player p = e.getPlayer();
        e.setCancelled(true);
        e.setDropItems(false);

        ItemStack tool = p.getInventory().getItemInMainHand();
        boolean hasSilkTouch = tool != null && tool.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH);
        if (!hasSilkTouch) {
            p.sendMessage("§8┃ §c§l✖ SPAWNER §8┃ §cDu brauchst §eSilk Touch§c!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        int breakAmount = p.isSneaking() ? Math.min(64, ps.getStackSize()) : 1;
        int removed = manager.removeFromStack(loc, breakAmount);
        if (removed > 0) {
            // Prüfe ob Spawner komplett weg ist
            if (manager.getPlacedSpawner(loc) == null) {
                block.setType(Material.AIR);
                List<ItemStack> storedDrops = ps.collectDrops();
                for (ItemStack drop : storedDrops) {
                    HashMap<Integer, ItemStack> dropLeftover = p.getInventory().addItem(drop);
                    for (ItemStack left : dropLeftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
                }
                if (!storedDrops.isEmpty())
                    p.sendMessage("§8┃ §7Gespeicherte Drops§8: §e" + storedDrops.stream().mapToInt(ItemStack::getAmount).sum() + " Items");
            }

            for (ItemStack spawnerItem : manager.createSpawnerItemStacks(ps.getType(), removed)) {
                HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(spawnerItem);
                for (ItemStack left : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
            }

            int remaining = manager.getPlacedSpawner(loc) != null ? manager.getPlacedSpawner(loc).getStackSize() : 0;
            p.sendMessage("");
            p.sendMessage("§8┃ §6§l⚡ SPAWNER §8┃ §e" + removed + "x " + ps.getType().getDisplayName() + " §7abgebaut!");
            if (remaining > 0) p.sendMessage("§8┃ §7Verbleibend§8: §a" + remaining + "x");
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 0.5f, 1f);
        }
    }

    // === RIGHT-CLICK INTERACTION ===
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.SPAWNER) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Location loc = e.getClickedBlock().getLocation();
        PlacedSpawner ps = manager.getPlacedSpawner(loc);
        if (ps == null) return;

        Player p = e.getPlayer();
        // Wenn Spieler einen Spawner gleichen Typs in der Hand hält → zum Stack hinzufügen
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() == Material.SPAWNER) {
            SpawnerType handType = manager.getSpawnerType(hand);
            if (handType != null && handType == ps.getType()) {
                e.setCancelled(true);
                int logicalStackPerItem = Math.max(1, manager.getSpawnerStack(hand));
                int consumedItems = p.isSneaking() ? hand.getAmount() : 1;
                int addAmount = consumedItems * logicalStackPerItem;
                ps.setStackSize(ps.getStackSize() + addAmount);
                manager.saveData();
                p.sendMessage("");
                p.sendMessage("§8┃ §6§l⚡ SPAWNER §8┃ §a" + addAmount + "x " + ps.getType().getDisplayName() + " §7hinzugefügt!");
                p.sendMessage("§8┃ §7Anzahl§8: §a" + ps.getStackSize() + "x");
                p.sendMessage("");
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);
                if (hand.getAmount() > consumedItems) hand.setAmount(hand.getAmount() - consumedItems);
                else p.getInventory().setItemInMainHand(null);
                return;
            }
        }

        e.setCancelled(true);
        new SpawnerGUI(plugin, ps).open(p);
    }

    // === GUI CLICK HANDLER ===
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SpawnerGUI)) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        SpawnerGUI gui = (SpawnerGUI) e.getInventory().getHolder();
        PlacedSpawner spawner = gui.getSpawner();

        NamespacedKey actionKey = new NamespacedKey(plugin, "spawner_action");
        if (clicked.getItemMeta().getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
            switch (action) {
                case "collect_all": collectAllDrops(p, spawner); p.closeInventory(); break;
                case "sell_all": sellAllDrops(p, spawner); p.closeInventory(); break;
                case "close": p.closeInventory(); p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); break;
            }
            return;
        }

        NamespacedKey dropKey = new NamespacedKey(plugin, "spawner_drop_index");
        if (clicked.getItemMeta().getPersistentDataContainer().has(dropKey, PersistentDataType.INTEGER)) {
            Integer index = clicked.getItemMeta().getPersistentDataContainer().get(dropKey, PersistentDataType.INTEGER);
            if (index != null) {
                List<ItemStack> drops = spawner.collectDrops();
                if (index < drops.size()) {
                    ItemStack drop = drops.get(index);
                    HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(drop);
                    for (ItemStack left : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
                    drops.remove((int) index);
                    spawner.addDrops(drops);
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1f);
                    gui.open(p);
                }
            }
        }
    }

    private void collectAllDrops(Player p, PlacedSpawner spawner) {
        List<ItemStack> drops = spawner.collectDrops();
        if (drops.isEmpty()) {
            p.sendMessage("§8┃ §e§l⚡ SPAWNER §8┃ §7Keine Drops vorhanden!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        int totalItems = 0;
        for (ItemStack drop : drops) {
            totalItems += drop.getAmount();
            HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(drop);
            for (ItemStack left : leftover.values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
        p.sendMessage("");
        p.sendMessage("§8┃ §a§l✓ EINGESAMMELT §8┃ §e" + totalItems + " Items");
        p.sendMessage("");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    private void sellAllDrops(Player p, PlacedSpawner spawner) {
        List<ItemStack> drops = spawner.collectDrops();
        if (drops.isEmpty()) {
            p.sendMessage("§8┃ §6§l💰 SELL §8┃ §7Keine Drops zum Verkaufen!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        double totalEarned = 0.0;
        int totalItems = 0;
        int itemsSold = 0;
        java.util.ArrayList<ItemStack> unsold = new java.util.ArrayList<>();
        for (ItemStack drop : drops) {
            double worth = plugin.getWorthManager().getWorth(drop);
            if (worth > 0) { totalEarned += worth * drop.getAmount(); totalItems += drop.getAmount(); itemsSold++; }
            else unsold.add(drop);
        }
        if (!unsold.isEmpty()) spawner.addDrops(unsold);
        if (totalEarned <= 0) {
            p.sendMessage("§8┃ §6§l💰 SELL §8┃ §cKeine Items konnten verkauft werden!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        // Sell Multiplier - nur verkaufte Items tracken
        java.util.List<ItemStack> soldItems = new java.util.ArrayList<>();
        for (ItemStack drop : drops) {
            if (plugin.getWorthManager().getWorth(drop) > 0) soldItems.add(drop);
        }
        double bonus = plugin.getSellMultiplier().trackSaleBatch(p, soldItems,
                item -> plugin.getWorthManager().getWorth(item));
        double finalTotal = totalEarned + bonus;
        plugin.getEconomy().deposit(p.getUniqueId(), finalTotal);
        p.sendMessage("");
        p.sendMessage("§8┃ §6§l💰 SPAWNER SELL §8┃ §a§l✓ VERKAUFT!");
        p.sendMessage("§8┃ §7Items§8: §e" + totalItems + " §7(" + itemsSold + " Stacks)");
        p.sendMessage("§8┃ §7Erhalten§8: §a+" + NumberFormatter.formatMoney(finalTotal)
                + (bonus > 0 ? " §8(§7+" + NumberFormatter.formatMoney(bonus) + " Bonus§8)" : ""));
        p.sendMessage("§8┃ §7Kontostand§8: §e" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(p.getUniqueId())));
        if (!unsold.isEmpty()) {
            int unsoldCount = unsold.stream().mapToInt(ItemStack::getAmount).sum();
            p.sendMessage("§8┃ §c" + unsoldCount + " Items nicht verkaufbar (zurück im Spawner)");
        }
        p.sendMessage("");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SpawnerGUI) e.setCancelled(true);
    }
}
