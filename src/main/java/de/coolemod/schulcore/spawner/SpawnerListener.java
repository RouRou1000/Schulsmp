package de.coolemod.schulcore.spawner;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.utils.NumberFormatter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

import java.util.List;
import java.util.UUID;

/**
 * Listener für das Spawner-System
 * Handhabt Platzierung, Interaktion und Abbau
 */
public class SpawnerListener implements Listener {
    private final SchulCorePlugin plugin;
    private final SpawnerSystemManager manager;

    public SpawnerListener(SchulCorePlugin plugin, SpawnerSystemManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * Spawner platzieren
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (item.getType() != Material.SPAWNER) return;
        
        SpawnerType type = manager.getSpawnerType(item);
        if (type == null) return; // Kein DonutSMP-Spawner
        
        Location loc = e.getBlock().getLocation();
        Player p = e.getPlayer();
        int stack = manager.getSpawnerStack(item);
        
        // Registriere im System OHNE Event zu canceln
        // Der Block wird normal platziert, wir registrieren ihn nur zusätzlich
        if (manager.placeSpawner(loc, type, stack, p.getUniqueId())) {
            p.sendMessage("");
            p.sendMessage("§8┃ §6§l⚡ SPAWNER §8┃ §a" + type.getDisplayName() + " §7platziert!");
            if (stack > 1) {
                p.sendMessage("§8┃ §7Stack§8: §a" + stack + "x");
            }
            p.sendMessage("§8┃ §7Rechtsklick zum Sammeln der Drops");
            p.sendMessage("§8┃ §e⚡ Shift+Rechtsklick§7 zum Stacken");
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.2f);
        } else {
            e.setCancelled(true);
            p.sendMessage("§8┃ §c§l✖ FEHLER §8┃ §cSpawner konnte nicht platziert werden!");
        }
    }

    /**
     * Spawner abbauen
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        Block block = e.getBlock();
        if (block.getType() != Material.SPAWNER) return;
        
        Location loc = block.getLocation();
        PlacedSpawner ps = manager.getPlacedSpawner(loc);
        if (ps == null) return; // Kein DonutSMP-Spawner
        
        Player p = e.getPlayer();
        e.setCancelled(true);
        e.setDropItems(false);
        
        // Prüfe Silk Touch
        ItemStack tool = p.getInventory().getItemInMainHand();
        boolean hasSilkTouch = tool != null && 
            tool.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH);
        
        if (!hasSilkTouch) {
            p.sendMessage("§8┃ §c§l✖ SPAWNER §8┃ §cDu brauchst §eSilk Touch§c!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        
        // Entferne Spawner
        ItemStack spawnerItem = manager.removeSpawner(loc);
        if (spawnerItem != null) {
            block.setType(Material.AIR);
            
            // Gib gespeicherte Drops
            List<ItemStack> storedDrops = ps.collectDrops();
            
            // Gib Spawner-Item
            java.util.HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(spawnerItem);
            for (ItemStack left : leftover.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), left);
            }
            
            // Gib gespeicherte Drops
            for (ItemStack drop : storedDrops) {
                java.util.HashMap<Integer, ItemStack> dropLeftover = p.getInventory().addItem(drop);
                for (ItemStack left : dropLeftover.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), left);
                }
            }
            
            p.sendMessage("");
            p.sendMessage("§8┃ §6§l⚡ SPAWNER §8┃ §c" + ps.getType().getDisplayName() + " §7abgebaut!");
            p.sendMessage("§8┃ §7Stack§8: §a" + ps.getStackSize() + "x");
            if (!storedDrops.isEmpty()) {
                p.sendMessage("§8┃ §7Gespeicherte Drops§8: §e" + storedDrops.stream().mapToInt(ItemStack::getAmount).sum() + " Items");
            }
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_DESTROY, 0.5f, 1f);
        }
    }

    /**
     * Spawner-Interaktion (Rechtsklick)
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;
        if (e.getClickedBlock().getType() != Material.SPAWNER) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        Location loc = e.getClickedBlock().getLocation();
        PlacedSpawner ps = manager.getPlacedSpawner(loc);
        if (ps == null) return; // Kein DonutSMP-Spawner
        
        e.setCancelled(true);
        Player p = e.getPlayer();
        
        // Shift+Rechtsklick = Stacken
        if (p.isSneaking()) {
            handleStacking(p, loc, ps);
            return;
        }
        
        // Normaler Rechtsklick = GUI öffnen
        SpawnerGUI gui = new SpawnerGUI(plugin, ps);
        gui.open(p);
    }

    /**
     * Handhabt das Stacken von Spawnern
     */
    private void handleStacking(Player p, Location loc, PlacedSpawner existing) {
        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (inHand.getType() != Material.SPAWNER) {
            p.sendMessage("§8┃ §e§l⚡ STACKING §8┃ §7Halte einen Spawner zum Stacken!");
            return;
        }
        
        SpawnerType handType = manager.getSpawnerType(inHand);
        if (handType == null) {
            p.sendMessage("§8┃ §c§l✖ STACKING §8┃ §cKein gültiger Spawner!");
            return;
        }
        
        if (handType != existing.getType()) {
            p.sendMessage("§8┃ §c§l✖ STACKING §8┃ §cNur gleiche Spawner-Typen!");
            p.sendMessage("§8┃ §7Platziert§8: " + existing.getType().getDisplayName());
            p.sendMessage("§8┃ §7In Hand§8: " + handType.getDisplayName());
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        
        int addStack = manager.getSpawnerStack(inHand);
        int oldStack = existing.getStackSize();
        
        if (manager.stackSpawner(loc, handType, addStack)) {
            // Entferne Item aus Hand
            if (inHand.getAmount() > 1) {
                inHand.setAmount(inHand.getAmount() - 1);
            } else {
                p.getInventory().setItemInMainHand(null);
            }
            
            p.sendMessage("");
            p.sendMessage("§8┃ §a§l✓ GESTACKT §8┃ " + existing.getType().getDisplayName());
            p.sendMessage("§8┃ §7Vorher§8: §e" + oldStack + "x");
            p.sendMessage("§8┃ §7Nachher§8: §a" + existing.getStackSize() + "x");
            p.sendMessage("§8┃ §e⚡ Mehr Stack = Mehr Drops!");
            p.sendMessage("");
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        } else {
            p.sendMessage("§8┃ §c§l✖ STACKING §8┃ §cFehler beim Stacken!");
        }
    }

    /**
     * GUI Click Handler
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof SpawnerGUI)) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        
        // Blockiere Klicks im eigenen Inventar
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;
        
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        
        SpawnerGUI gui = (SpawnerGUI) e.getInventory().getHolder();
        PlacedSpawner spawner = gui.getSpawner();
        
        NamespacedKey actionKey = new NamespacedKey(plugin, "spawner_action");
        if (clicked.getItemMeta().getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
            
            switch (action) {
                case "collect_all":
                    collectAllDrops(p, spawner);
                    p.closeInventory();
                    break;
                case "sell_all":
                    sellAllDrops(p, spawner);
                    p.closeInventory();
                    break;
                case "close":
                    p.closeInventory();
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                    break;
            }
            return;
        }
        
        // Einzelnen Drop einsammeln
        NamespacedKey dropKey = new NamespacedKey(plugin, "spawner_drop_index");
        if (clicked.getItemMeta().getPersistentDataContainer().has(dropKey, PersistentDataType.INTEGER)) {
            Integer index = clicked.getItemMeta().getPersistentDataContainer().get(dropKey, PersistentDataType.INTEGER);
            if (index != null) {
                // Sammle diesen Drop
                List<ItemStack> drops = spawner.collectDrops();
                if (index < drops.size()) {
                    ItemStack drop = drops.get(index);
                    java.util.HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(drop);
                    for (ItemStack left : leftover.values()) {
                        p.getWorld().dropItemNaturally(p.getLocation(), left);
                    }
                    
                    // Füge verbleibende zurück
                    drops.remove((int) index);
                    spawner.addDrops(drops);
                    
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1f);
                    
                    // Refresh GUI
                    gui.open(p);
                }
            }
        }
    }

    /**
     * Sammelt alle Drops ein
     */
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
            java.util.HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(drop);
            for (ItemStack left : leftover.values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), left);
            }
        }
        
        p.sendMessage("");
        p.sendMessage("§8┃ §a§l✓ EINGESAMMELT §8┃ §e" + totalItems + " Items");
        p.sendMessage("");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
    }

    /**
     * Verkauft alle Drops direkt
     */
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
        List<ItemStack> unsold = new java.util.ArrayList<>();
        
        for (ItemStack drop : drops) {
            double worth = plugin.getWorthManager().getWorth(drop);
            if (worth > 0) {
                totalEarned += worth * drop.getAmount();
                totalItems += drop.getAmount();
                itemsSold++;
            } else {
                // Nicht verkaufbar - gib zurück ins Inventar
                unsold.add(drop);
            }
        }
        
        // Nicht verkaufte Items zurück in den Spawner
        if (!unsold.isEmpty()) {
            spawner.addDrops(unsold);
        }
        
        if (totalEarned <= 0) {
            p.sendMessage("§8┃ §6§l💰 SELL §8┃ §cKeine Items konnten verkauft werden!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        
        // Geld gutschreiben
        plugin.getEconomy().deposit(p.getUniqueId(), totalEarned);
        
        // Shards pro 1k verdient
        int shardsEarned = NumberFormatter.calculateShardsFromMoney(totalEarned);
        if (shardsEarned > 0) {
            plugin.getShards().addShards(p.getUniqueId(), shardsEarned);
        }
        
        p.sendMessage("");
        p.sendMessage("§8┃ §6§l💰 SPAWNER SELL §8┃ §a§l✓ VERKAUFT!");
        p.sendMessage("§8┃ §7Items§8: §e" + totalItems + " §7(" + itemsSold + " Stacks)");
        p.sendMessage("§8┃ §7Erhalten§8: §a+" + NumberFormatter.formatMoney(totalEarned));
        if (shardsEarned > 0) {
            p.sendMessage("§8┃ §7Shards§8: §d+" + shardsEarned + " §8(§71 pro $1k§8)");
        }
        p.sendMessage("§8┃ §7Kontostand§8: §e" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(p.getUniqueId())));
        if (!unsold.isEmpty()) {
            int unsoldCount = unsold.stream().mapToInt(ItemStack::getAmount).sum();
            p.sendMessage("§8┃ §c" + unsoldCount + " Items nicht verkaufbar (zurück im Spawner)");
        }
        p.sendMessage("");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
    }

    /**
     * Verhindere Drag in Spawner-GUI
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof SpawnerGUI) {
            e.setCancelled(true);
        }
    }
}
