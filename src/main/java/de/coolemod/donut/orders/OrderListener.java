package de.coolemod.donut.orders;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Event Handler für Order System
 */
public class OrderListener implements Listener {
    private final DonutPlugin plugin;
    private final OrderSystem orderSystem;
    private int currentPage = 0;
    
    public OrderListener(DonutPlugin plugin, OrderSystem orderSystem) {
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
        
        // Allow player inventory clicks
        if (slot >= e.getView().getTopInventory().getSize()) {
            // NEW ORDER GUI: Allow taking items from inventory
            if (title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) {
                e.setCancelled(false);
            } else {
                e.setCancelled(true);
            }
            return;
        }
        
        // NEW ORDER GUI: Slot 4 is completely free like a chest slot
        if (title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ") && slot == 4) {
            e.setCancelled(false);
            // Update session after inventory change
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack item = e.getView().getTopInventory().getItem(4);
                OrderSystem.CreateSession session = orderSystem.getCreateSession(player.getUniqueId());
                if (session != null) {
                    if (item != null && !item.getType().isAir()) {
                        session.item = item.clone();
                    } else {
                        session.item = null;
                        session.amountSet = false;
                        session.priceSet = false;
                    }
                }
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
                    
                    int needed = order.requiredAmount - order.delivered;
                    int inInventory = 0;
                    for (ItemStack item : player.getInventory().getContents()) {
                        if (item != null && item.isSimilar(order.itemType)) {
                            inInventory += item.getAmount();
                        }
                    }
                    
                    if (inInventory <= 0) {
                        player.sendMessage("§8§m                    ");
                        player.sendMessage("§c✗ " + orderSystem.toSmallCaps("NICHT GENUG ITEMS"));
                        player.sendMessage("§8");
                        player.sendMessage("§7Du hast keine passenden Items");
                        player.sendMessage("§7in deinem Inventar.");
                        player.sendMessage("§8§m                    ");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                    
                    int toDeliver = Math.min(needed, inInventory);
                    if (orderSystem.deliverToOrder(orderId, player, toDeliver)) {
                        double payment = toDeliver * order.pricePerItem;
                        player.sendMessage("§8§m                    ");
                        player.sendMessage("§a✓ " + orderSystem.toSmallCaps("BELIEFERT"));
                        player.sendMessage("§8");
                        player.sendMessage("§7Menge: §f" + toDeliver + "x");
                        player.sendMessage("§6⛃ §7Erhalten: §a§l+$" + String.format("%.2f", payment));
                        player.sendMessage("§6⛃ §7Kontostand: §e$" + String.format("%.2f", plugin.getEconomy().getBalance(player.getUniqueId())));
                        player.sendMessage("§8§m                    ");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                        
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.openInventory(orderSystem.createBrowseGUI(currentPage));
                        }, 2L);
                    }
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
                player.openInventory(orderSystem.createBrowseGUI(currentPage));
                break;
                
            case "next":
                currentPage++;
                player.openInventory(orderSystem.createBrowseGUI(currentPage));
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
                    player.openInventory(orderSystem.createBrowseGUI(0));
                }, 2L);
                break;
                
            case "set_amount":
                OrderSystem.CreateSession amountSession = orderSystem.getCreateSession(player.getUniqueId());
                if (amountSession != null && amountSession.item != null) {
                    openAmountSignGUI(player);
                }
                break;
                
            case "set_price":
                OrderSystem.CreateSession priceSession = orderSystem.getCreateSession(player.getUniqueId());
                if (priceSession != null && priceSession.item != null) {
                    openPriceSignGUI(player);
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
                            player.openInventory(orderSystem.createBrowseGUI(0));
                        }, 2L);
                    }
                }
                break;
        }
    }
    
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (orderSystem.isOrderGUI(title)) {
            e.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        
        String title = e.getView().getTitle();
        if (title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) {
            // Don't cleanup yet, player might be opening sign
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.getOpenInventory().getTitle().contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) {
                    orderSystem.endCreateSession(player.getUniqueId());
                }
            }, 5L);
        }
    }
    
    @EventHandler
    public void onSign(SignChangeEvent e) {
        Player player = e.getPlayer();
        OrderSystem.CreateSession session = orderSystem.getCreateSession(player.getUniqueId());
        
        if (session == null) return;
        
        String input = e.getLine(0);
        
        // Remove sign
        Bukkit.getScheduler().runTask(plugin, () -> {
            e.getBlock().setType(Material.AIR);
        });
        
        if (input == null || input.trim().isEmpty()) {
            player.sendMessage("§cKeine Eingabe!");
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
            });
            return;
        }
        
        // Check if setting amount or price
        if (!session.amountSet) {
            // Setting amount
            try {
                int amount = Integer.parseInt(input.trim());
                
                if (amount <= 0) {
                    player.sendMessage("§cMenge muss größer als 0 sein!");
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
                    });
                    return;
                }
                
                session.amount = amount;
                session.amountSet = true;
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
                    player.sendMessage("§a✓ Menge gesetzt: §f" + amount + "x");
                });
                
            } catch (NumberFormatException ex) {
                player.sendMessage("§cUngültige Zahl!");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
                });
            }
        } else if (!session.priceSet) {
            // Setting price
            try {
                double price = Double.parseDouble(input.trim().replace(",", "."));
                
                if (price <= 0) {
                    player.sendMessage("§cPreis muss größer als 0 sein!");
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
                    });
                    return;
                }
                
                session.price = price;
                session.priceSet = true;
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
                    player.sendMessage("§a✓ Preis gesetzt: §e$" + String.format("%.2f", price) + "/Stück");
                });
                
            } catch (NumberFormatException ex) {
                player.sendMessage("§cUngültige Zahl!");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
                });
            }
        }
    }
    
    private void openAmountSignGUI(Player player) {
        try {
            org.bukkit.block.Block block = player.getLocation().add(0, 3, 0).getBlock();
            Material originalType = block.getType();
            
            block.setType(Material.OAK_SIGN);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setLine(0, "");
            sign.setLine(1, "^^^^^^^^^^^^^^");
            sign.setLine(2, "Menge eingeben");
            sign.setLine(3, "");
            sign.update(false, false);
            
            player.openSign(sign);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                block.setType(originalType);
            }, 200L);
            
        } catch (Exception ex) {
            player.sendMessage("§cFehler beim Öffnen der Mengen-Eingabe!");
        }
    }
    
    private void openPriceSignGUI(Player player) {
        try {
            org.bukkit.block.Block block = player.getLocation().add(0, 3, 0).getBlock();
            Material originalType = block.getType();
            
            block.setType(Material.OAK_SIGN);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setLine(0, "");
            sign.setLine(1, "^^^^^^^^^^^^^^");
            sign.setLine(2, "Preis/Stück");
            sign.setLine(3, "");
            sign.update(false, false);
            
            player.openSign(sign);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                block.setType(originalType);
            }, 200L);
            
        } catch (Exception ex) {
            player.sendMessage("§cFehler beim Öffnen der Preis-Eingabe!");
        }
    }
}
