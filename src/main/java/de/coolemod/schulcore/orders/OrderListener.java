package de.coolemod.schulcore.orders;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;

/**
 * Event Handler für Order System
 */
public class OrderListener implements Listener {
    private final SchulCorePlugin plugin;
    private final OrderSystem orderSystem;
    private int currentPage = 0;
    
    public OrderListener(SchulCorePlugin plugin, OrderSystem orderSystem) {
        this.plugin = plugin;
        this.orderSystem = orderSystem;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        
        String title = e.getView().getTitle();
        if (!orderSystem.isOrderGUI(title)) return;
        
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor = e.getCursor();
        
        // === DELIVER GUI: Freie Item-Platzierung ===
        boolean isDeliverGUI = title.contains("ᴏʀᴅᴇʀ ʟɪᴇғᴇʀɴ");
        if (isDeliverGUI) {
            // Slots 9-44 sind frei
            if (slot >= 9 && slot <= 44) {
                e.setCancelled(false);
                // Auszahlung aktualisieren nach Klick
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    OrderSystem.DeliverSession ds = orderSystem.getDeliverSession(player.getUniqueId());
                    if (ds != null) {
                        orderSystem.updateDeliverInfo(e.getView().getTopInventory(), ds.orderId);
                    }
                }, 1L);
                return;
            }
            // Spieler-Inventar erlauben
            if (slot >= e.getView().getTopInventory().getSize()) {
                e.setCancelled(false);
                if (e.isShiftClick()) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        OrderSystem.DeliverSession ds = orderSystem.getDeliverSession(player.getUniqueId());
                        if (ds != null) {
                            orderSystem.updateDeliverInfo(e.getView().getTopInventory(), ds.orderId);
                        }
                    }, 1L);
                }
                return;
            }
            // Aktions-Buttons in der Deliver-GUI
            e.setCancelled(true);
            String action = orderSystem.getAction(clicked);
            if (action == null) return;
            switch (action) {
                case "deliver_confirm" -> {
                    OrderSystem.DeliverSession ds = orderSystem.getDeliverSession(player.getUniqueId());
                    if (ds != null) {
                        OrderSystem.DeliverResult result = orderSystem.processDelivery(player, e.getView().getTopInventory(), ds.orderId);
                        orderSystem.endDeliverSession(player.getUniqueId());
                        player.closeInventory();
                        if (result.success()) {
                            player.sendMessage("§8§m                    ");
                            player.sendMessage("§a✓ " + orderSystem.toSmallCaps("BELIEFERT"));
                            player.sendMessage("§8");
                            player.sendMessage("§7Menge: §f" + result.delivered() + "x");
                            player.sendMessage("§6⛃ §7Erhalten: §a§l+$" + String.format("%.2f", result.payment()));
                            player.sendMessage("§6⛃ §7Kontostand: §e$" + String.format("%.2f", plugin.getEconomy().getBalance(player.getUniqueId())));
                            player.sendMessage("§8§m                    ");
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                        } else {
                            player.sendMessage("§8§m                    ");
                            player.sendMessage("§c✗ " + orderSystem.toSmallCaps("KEINE PASSENDEN ITEMS"));
                            player.sendMessage("§8");
                            player.sendMessage("§7Keine passenden Items gefunden.");
                            player.sendMessage("§8§m                    ");
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        }
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.openInventory(orderSystem.createBrowseGUI(currentPage, player.getUniqueId()));
                        }, 2L);
                    }
                }
                case "deliver_back" -> {
                    // Items zurückgeben
                    returnDeliverItems(player, e.getView().getTopInventory());
                    orderSystem.endDeliverSession(player.getUniqueId());
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.openInventory(orderSystem.createBrowseGUI(currentPage, player.getUniqueId()));
                    }, 2L);
                }
            }
            return;
        }
        
        // Block dangerous click types in browse/my_orders GUIs (prevent item loss)
        if (!title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) {
            InventoryAction iaction = e.getAction();
            if (iaction == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
                iaction == InventoryAction.COLLECT_TO_CURSOR ||
                iaction == InventoryAction.HOTBAR_SWAP ||
                iaction == InventoryAction.HOTBAR_MOVE_AND_READD) {
                e.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, player::updateInventory, 1L);
                return;
            }
        }
        
        // Allow player inventory clicks
        if (slot >= e.getView().getTopInventory().getSize()) {
            // NEW ORDER GUI: Allow taking items from inventory
            if (title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) {
                e.setCancelled(false);
                // Handle shift-click: item might go to slot 4
                if (e.isShiftClick()) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        updateSlot4Session(player, e.getView().getTopInventory());
                    }, 1L);
                }
            } else {
                e.setCancelled(true);
            }
            return;
        }
        
        // NEW ORDER GUI: Slot 4 is completely free like a chest slot
        if (title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ") && slot == 4) {
            e.setCancelled(false);
            // Update session and refresh buttons after inventory change
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                updateSlot4Session(player, e.getView().getTopInventory());
            }, 1L);
            return;
        }
        
        // Cancel all other top inventory clicks
        e.setCancelled(true);
        
        String action = orderSystem.getAction(clicked);
        if (action == null) {
            return;
        }
        
        // Handle actions
        switch (action) {
            case "border":
            case "disabled":
                break;
                
            case "deliver":
                String orderId = orderSystem.getOrderId(clicked);
                if (orderId != null) {
                    OrderSystem.Order order = orderSystem.getOrders().stream()
                        .filter(o -> o.id.equals(orderId))
                        .findFirst()
                        .orElse(null);
                    
                    if (order == null) {
                        player.sendMessage("§cOrder nicht gefunden!");
                        return;
                    }
                    
                    // Can't deliver to own order
                    if (order.owner.equals(player.getUniqueId())) {
                        player.sendMessage("§8§m                    ");
                        player.sendMessage("§c✗ " + orderSystem.toSmallCaps("EIGENE ORDER"));
                        player.sendMessage("§8");
                        player.sendMessage("§7Du kannst deine eigene");
                        player.sendMessage("§7Order nicht beliefern!");
                        player.sendMessage("§8§m                    ");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                    
                    // Deliver-GUI öffnen
                    orderSystem.startDeliverSession(player.getUniqueId(), orderId);
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        org.bukkit.inventory.Inventory deliverGUI = orderSystem.createDeliverGUI(orderId);
                        if (deliverGUI != null) {
                            player.openInventory(deliverGUI);
                        } else {
                            player.sendMessage("§cOrder nicht mehr verfügbar!");
                            orderSystem.endDeliverSession(player.getUniqueId());
                        }
                    }, 2L);
                }
                break;
                
            case "cancel":
                String cancelId = orderSystem.getOrderId(clicked);
                if (cancelId != null) {
                    if (orderSystem.cancelOrder(player, cancelId)) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.openInventory(orderSystem.createMyOrdersGUI(player.getUniqueId()));
                        }, 2L);
                    }
                }
                break;
                
            case "new":
                orderSystem.startCreateSession(player.getUniqueId());
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
                }, 2L);
                break;
                
            case "prev":
                currentPage--;
                player.openInventory(orderSystem.createBrowseGUI(currentPage, player.getUniqueId()));
                break;
                
            case "next":
                currentPage++;
                player.openInventory(orderSystem.createBrowseGUI(currentPage, player.getUniqueId()));
                break;
                
            case "my_orders":
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(orderSystem.createMyOrdersGUI(player.getUniqueId()));
                }, 2L);
                break;
                
            case "back":
                orderSystem.endCreateSession(player.getUniqueId());
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(orderSystem.createBrowseGUI(0, player.getUniqueId()));
                }, 2L);
                break;
                
            // ===== SEARCH & SORT ACTIONS =====
            case "search":
                if (e.isLeftClick()) {
                    // Open chat search input
                    player.closeInventory();
                    player.setMetadata("order_search_input", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    player.sendMessage("§8┃ §d§lORDER §8┃ §7Bitte schreibe den §eSuchbegriff §7in den Chat. Abbrechen mit §cstop§7.");
                } else if (e.isRightClick()) {
                    OrderSystem.BrowseSession browseSession = orderSystem.getBrowseSession(player.getUniqueId());
                    if (browseSession.searchQuery != null) {
                        browseSession.searchQuery = null;
                        player.sendMessage("§8┃ §d§lORDER §8┃ §7Suche gelöscht.");
                        currentPage = 0;
                        player.openInventory(orderSystem.createBrowseGUI(currentPage, player.getUniqueId()));
                    }
                }
                break;
                
            case "sort":
                OrderSystem.BrowseSession sortSession = orderSystem.getBrowseSession(player.getUniqueId());
                sortSession.sortMode = sortSession.sortMode.next();
                player.sendMessage("§8┃ §d§lORDER §8┃ §7Sortierung: §e" + sortSession.sortMode.display);
                currentPage = 0;
                player.openInventory(orderSystem.createBrowseGUI(currentPage, player.getUniqueId()));
                break;
                
            case "set_amount":
                OrderSystem.CreateSession amountSession = orderSystem.getCreateSession(player.getUniqueId());
                if (amountSession != null && amountSession.item != null) {
                    player.closeInventory();
                    player.setMetadata("order_input_amount", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    player.sendMessage("§8┃ §d§lORDER §8┃ §7Bitte schreibe die §eMenge §7in den Chat (z.B. §e64§7, §e1k§7). Abbrechen mit §cstop§7.");
                }
                break;
                
            case "set_price":
                OrderSystem.CreateSession priceSession = orderSystem.getCreateSession(player.getUniqueId());
                if (priceSession != null && priceSession.item != null) {
                    player.closeInventory();
                    player.setMetadata("order_input_price", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    player.sendMessage("§8┃ §d§lORDER §8┃ §7Bitte schreibe den §ePreis pro Item §7in den Chat (z.B. §e150§7, §e2.5k§7). Abbrechen mit §cstop§7.");
                }
                break;
                
            case "confirm":
                OrderSystem.CreateSession confirmSession = orderSystem.getCreateSession(player.getUniqueId());
                if (confirmSession != null && confirmSession.priceSet && confirmSession.amountSet && confirmSession.item != null) {
                    double total = confirmSession.amount * confirmSession.price;
                    if (plugin.getEconomy().getBalance(player.getUniqueId()) < total) {
                        player.sendMessage("§8§m                    ");
                        player.sendMessage("§c✗ " + orderSystem.toSmallCaps("NICHT GENUG GELD"));
                        player.sendMessage("§8");
                        player.sendMessage("§7Benötigt: §e$" + String.format("%.2f", total));
                        player.sendMessage("§7Kontostand: §e$" + String.format("%.2f", plugin.getEconomy().getBalance(player.getUniqueId())));
                        player.sendMessage("§8§m                    ");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                    
                    String id = orderSystem.createOrder(player.getUniqueId(), confirmSession.item, confirmSession.amount, confirmSession.price);
                    if (id != null) {
                        player.sendMessage("§8§m                    ");
                        player.sendMessage("§a✓ " + orderSystem.toSmallCaps("ORDER ERSTELLT"));
                        player.sendMessage("§8");
                        player.sendMessage("§7Item: §f" + confirmSession.item.getType().name());
                        player.sendMessage("§7Menge: §f" + confirmSession.amount + "x");
                        player.sendMessage("§7Preis/Stück: §e$" + String.format("%.2f", confirmSession.price));
                        player.sendMessage("§8");
                        player.sendMessage("§6⛃ §7Reserviert: §c-$" + String.format("%.2f", total));
                        player.sendMessage("§6⛃ §7Kontostand: §e$" + String.format("%.2f", plugin.getEconomy().getBalance(player.getUniqueId())));
                        player.sendMessage("§8§m                    ");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                        
                        confirmSession.item = null; // Prevent item duplication
                        orderSystem.endCreateSession(player.getUniqueId());
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.openInventory(orderSystem.createBrowseGUI(0, player.getUniqueId()));
                        }, 2L);
                    }
                }
                break;
        }
    }
    
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (!orderSystem.isOrderGUI(title)) return;
        
        // Deliver GUI: erlaube Drags in freie Slots (9-44)
        if (title.contains("ᴏʀᴅᴇʀ ʟɪᴇғᴇʀɴ")) {
            boolean allInFreeArea = e.getRawSlots().stream().allMatch(s -> s >= 9 && s <= 44);
            if (allInFreeArea) {
                e.setCancelled(false);
                Player player = (Player) e.getWhoClicked();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    OrderSystem.DeliverSession ds = orderSystem.getDeliverSession(player.getUniqueId());
                    if (ds != null) {
                        orderSystem.updateDeliverInfo(e.getView().getTopInventory(), ds.orderId);
                    }
                }, 1L);
                return;
            }
        }
        
        e.setCancelled(true);
    }
    
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        
        String title = e.getView().getTitle();
        
        // Deliver GUI: Items zurückgeben bei Close
        if (title.contains("ᴏʀᴅᴇʀ ʟɪᴇғᴇʀɴ")) {
            OrderSystem.DeliverSession ds = orderSystem.getDeliverSession(player.getUniqueId());
            if (ds != null) {
                returnDeliverItems(player, e.getView().getTopInventory());
                orderSystem.endDeliverSession(player.getUniqueId());
            }
            return;
        }
        
        if (title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) {
            // Don't cleanup yet, player might be doing chat input or reopening GUI
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Don't end session if player is doing chat input
                if (player.hasMetadata("order_input_amount") || player.hasMetadata("order_input_price")) {
                    return;
                }
                if (!player.getOpenInventory().getTitle().contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) {
                    orderSystem.endCreateSession(player.getUniqueId());
                }
            }, 5L);
        }
    }
    
    @EventHandler
    public void onSign(SignChangeEvent e) {
        // Fallback: keine Sign-Eingabe mehr nötig (Chat-Input wird verwendet)
    }
    
    private void openAmountSignGUI(Player player) {
        try {
            org.bukkit.block.Block block = player.getLocation().add(0, 3, 0).getBlock();
            Material originalType = block.getType();
            
            block.setType(Material.OAK_SIGN, false);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setWaxed(false);
            org.bukkit.block.sign.SignSide front = sign.getSide(org.bukkit.block.sign.Side.FRONT);
            front.setLine(0, "");
            front.setLine(1, "^^^^^^^^^^^^^^");
            front.setLine(2, "Menge eingeben");
            front.setLine(3, "");
            sign.update(true, false);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() == Material.OAK_SIGN) {
                    player.openSign((org.bukkit.block.Sign) block.getState(), org.bukkit.block.sign.Side.FRONT);
                }
            }, 3L);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                block.setType(originalType);
            }, 200L);
            
        } catch (Exception ex) {
            player.sendMessage("§cFehler beim Öffnen der Mengen-Eingabe!");
        }
    }
    
    private void updateSlot4Session(Player player, Inventory topInventory) {
        // Only update if player still has the GUI open
        if (!player.getOpenInventory().getTitle().contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) return;
        
        ItemStack item = topInventory.getItem(4);
        OrderSystem.CreateSession session = orderSystem.getCreateSession(player.getUniqueId());
        if (session != null) {
            if (item != null && !item.getType().isAir()) {
                session.item = item.clone();
            } else {
                session.item = null;
                session.amountSet = false;
                session.priceSet = false;
            }
            orderSystem.updateNewOrderButtons(topInventory, session);
        }
    }
    
    private void returnDeliverItems(Player player, Inventory deliverInv) {
        for (int i = 9; i <= 44; i++) {
            ItemStack item = deliverInv.getItem(i);
            if (item != null && !item.getType().isAir()) {
                HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
                overflow.values().forEach(o -> player.getWorld().dropItem(player.getLocation(), o));
                deliverInv.setItem(i, null);
            }
        }
    }
    
    private void openPriceSignGUI(Player player) {
        try {
            org.bukkit.block.Block block = player.getLocation().add(0, 3, 0).getBlock();
            Material originalType = block.getType();
            
            block.setType(Material.OAK_SIGN, false);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setWaxed(false);
            org.bukkit.block.sign.SignSide front = sign.getSide(org.bukkit.block.sign.Side.FRONT);
            front.setLine(0, "");
            front.setLine(1, "^^^^^^^^^^^^^^");
            front.setLine(2, "Preis/Stück");
            front.setLine(3, "");
            sign.update(true, false);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() == Material.OAK_SIGN) {
                    player.openSign((org.bukkit.block.Sign) block.getState(), org.bukkit.block.sign.Side.FRONT);
                }
            }, 3L);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                block.setType(originalType);
            }, 200L);
            
        } catch (Exception ex) {
            player.sendMessage("§cFehler beim Öffnen der Preis-Eingabe!");
        }
    }
}
