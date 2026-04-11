package de.coolemod.donut.orders;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.metadata.MetadataValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Event Handler für Order System
 */
public class OrderListener implements Listener {
    private static final String ORDER_SIGN_MODE = "order_sign_mode";
    private static final String ORDER_SIGN_BLOCK = "order_sign_block";
    private static final String ORDER_SIGN_ORIGINAL = "order_sign_original";

    private final DonutPlugin plugin;
    private final OrderSystem orderSystem;

    public OrderListener(DonutPlugin plugin, OrderSystem orderSystem) {
        this.plugin = plugin;
        this.orderSystem = orderSystem;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        String title = e.getView().getTitle();

        // Handle delivery chest GUI separately (allows item placement)
        if (orderSystem.isDeliveryChestGUI(title)) {
            int slot = e.getRawSlot();
            // Protect info bar (slots 0-8)
            if (slot >= 0 && slot < 9) {
                e.setCancelled(true);
            }
            return;
        }

        if (!orderSystem.isOrderGUI(title)) return;

        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        // Allow player inventory clicks
        if (slot >= e.getView().getTopInventory().getSize()) {
            e.setCancelled(true);
            return;
        }

        // Cancel all top inventory clicks
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

                    if (order.owner.equals(player.getUniqueId())) {
                        player.sendMessage("§cDu kannst deine eigene Order nicht beliefern!");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }

                    Inventory deliveryInv = orderSystem.createDeliveryChestGUI(player, orderId);
                    if (deliveryInv != null) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.openInventory(deliveryInv);
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
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(orderSystem.createItemSelectGUI(player.getUniqueId(), 0));
                }, 2L);
                break;

            case "prev":
                player.openInventory(orderSystem.createBrowseGUI(orderSystem.getBrowseSession(player.getUniqueId()).page - 1, player.getUniqueId()));
                break;

            case "next":
                player.openInventory(orderSystem.createBrowseGUI(orderSystem.getBrowseSession(player.getUniqueId()).page + 1, player.getUniqueId()));
                break;

            case "search":
                if (e.isRightClick() && orderSystem.getBrowseSession(player.getUniqueId()).searchQuery != null) {
                    orderSystem.clearSearchQuery(player.getUniqueId());
                    player.openInventory(orderSystem.createBrowseGUI(0, player.getUniqueId()));
                    player.sendMessage("§8┃ §d§lORDER §8┃ §7Suche gelöscht.");
                } else {
                    player.closeInventory();
                    openInputSignGUI(player, "search", "Order Suche", "Item eingeben", orderSystem.getBrowseSession(player.getUniqueId()).searchQuery);
                }
                break;

            case "select_item":
                String matName = orderSystem.getMaterial(clicked);
                if (matName != null) {
                    Material selectedMat = Material.valueOf(matName);
                    orderSystem.startCreateSession(player.getUniqueId());
                    OrderSystem.CreateSession selectSession = orderSystem.getCreateSession(player.getUniqueId());
                    selectSession.item = new ItemStack(selectedMat);
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
                    }, 2L);
                }
                break;

            case "item_select_prev":
                player.openInventory(orderSystem.createItemSelectGUI(player.getUniqueId(),
                    orderSystem.getBrowseSession(player.getUniqueId()).itemSelectPage - 1));
                break;

            case "item_select_next":
                player.openInventory(orderSystem.createItemSelectGUI(player.getUniqueId(),
                    orderSystem.getBrowseSession(player.getUniqueId()).itemSelectPage + 1));
                break;

            case "item_select_search":
                if (e.isRightClick() && orderSystem.getBrowseSession(player.getUniqueId()).itemSelectSearch != null) {
                    orderSystem.getBrowseSession(player.getUniqueId()).itemSelectSearch = null;
                    player.openInventory(orderSystem.createItemSelectGUI(player.getUniqueId(), 0));
                    player.sendMessage("§8┃ §d§lORDER §8┃ §7Suche gelöscht.");
                } else {
                    player.closeInventory();
                    openInputSignGUI(player, "item_search", "Item Suche", "Material suchen", orderSystem.getBrowseSession(player.getUniqueId()).itemSelectSearch);
                }
                break;

            case "item_select_back":
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(orderSystem.createMyOrdersGUI(player.getUniqueId()));
                }, 2L);
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
                    player.openInventory(orderSystem.createItemSelectGUI(player.getUniqueId(), orderSystem.getBrowseSession(player.getUniqueId()).itemSelectPage));
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
                        player.sendMessage("§7Benötigt: §e" + NumberFormatter.formatMoney(total));
                        player.sendMessage("§7Kontostand: §e" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(player.getUniqueId())));
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
                        player.sendMessage("§7Preis/Stück: §e" + NumberFormatter.formatMoney(confirmSession.price));
                        player.sendMessage("§8");
                        player.sendMessage("§6⛃ §7Reserviert: §c-" + NumberFormatter.formatMoney(total));
                        player.sendMessage("§6⛃ §7Kontostand: §e" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(player.getUniqueId())));
                        player.sendMessage("§8§m                    ");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

                        confirmSession.item = null; // Prevent item duplication
                        orderSystem.endCreateSession(player.getUniqueId());
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.openInventory(orderSystem.createBrowseGUI(orderSystem.getBrowseSession(player.getUniqueId()).page, player.getUniqueId()));
                        }, 2L);
                    }
                }
                break;

            case "delivery_confirm":
                OrderSystem.DeliverySession confirmDelivery = orderSystem.getDeliverySession(player.getUniqueId());
                if (confirmDelivery != null && confirmDelivery.state == OrderSystem.DeliverySession.State.CONFIRMING) {
                    confirmDelivery.state = OrderSystem.DeliverySession.State.DONE;

                    OrderSystem.Order deliverOrder = orderSystem.getOrders().stream()
                        .filter(o -> o.id.equals(confirmDelivery.orderId))
                        .findFirst()
                        .orElse(null);

                    if (deliverOrder == null) {
                        giveItemsOrDrop(player, confirmDelivery.matchingItems);
                        player.sendMessage("§cOrder nicht mehr vorhanden! Items zurückgegeben.");
                        orderSystem.endDeliverySession(player.getUniqueId());
                        player.closeInventory();
                        return;
                    }

                    double payment = confirmDelivery.matchingCount * deliverOrder.pricePerItem;

                    if (orderSystem.completeDelivery(confirmDelivery.orderId, player, confirmDelivery.matchingCount)) {
                        player.sendMessage("§8§m                    ");
                        player.sendMessage("§a✓ " + orderSystem.toSmallCaps("BELIEFERT"));
                        player.sendMessage("§8");
                        player.sendMessage("§7Menge: §f" + confirmDelivery.matchingCount + "x");
                        player.sendMessage("§6⛃ §7Erhalten: §a§l+" + NumberFormatter.formatMoney(payment));
                        player.sendMessage("§6⛃ §7Kontostand: §e" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(player.getUniqueId())));
                        player.sendMessage("§8§m                    ");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                    }

                    orderSystem.endDeliverySession(player.getUniqueId());
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        int pg = orderSystem.getBrowseSession(player.getUniqueId()).page;
                        player.openInventory(orderSystem.createBrowseGUI(pg, player.getUniqueId()));
                    }, 2L);
                }
                break;

            case "delivery_cancel":
                OrderSystem.DeliverySession cancelDelivery = orderSystem.getDeliverySession(player.getUniqueId());
                if (cancelDelivery != null) {
                    cancelDelivery.state = OrderSystem.DeliverySession.State.DONE;
                    giveItemsOrDrop(player, cancelDelivery.matchingItems);
                    orderSystem.endDeliverySession(player.getUniqueId());

                    player.sendMessage("§c✗ Lieferung abgebrochen. Items zurückgegeben.");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);

                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        int pg = orderSystem.getBrowseSession(player.getUniqueId()).page;
                        player.openInventory(orderSystem.createBrowseGUI(pg, player.getUniqueId()));
                    }, 2L);
                }
                break;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();

        if (orderSystem.isDeliveryChestGUI(title)) {
            for (int slot : e.getRawSlots()) {
                if (slot >= 0 && slot < 9) {
                    e.setCancelled(true);
                    return;
                }
            }
            return;
        }

        if (orderSystem.isOrderGUI(title)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();

        String title = e.getView().getTitle();

        // Handle delivery chest close → process items
        if (orderSystem.isDeliveryChestGUI(title)) {
            OrderSystem.DeliverySession session = orderSystem.getDeliverySession(player.getUniqueId());
            if (session != null && session.state == OrderSystem.DeliverySession.State.DEPOSITING) {
                processDeliveryChest(player, e.getInventory());
            }
            return;
        }

        // Handle delivery confirm close without clicking → cancel
        if (orderSystem.isDeliveryConfirmGUI(title)) {
            OrderSystem.DeliverySession session = orderSystem.getDeliverySession(player.getUniqueId());
            if (session != null && session.state == OrderSystem.DeliverySession.State.CONFIRMING) {
                session.state = OrderSystem.DeliverySession.State.DONE;
                giveItemsOrDrop(player, session.matchingItems);
                orderSystem.endDeliverySession(player.getUniqueId());
                player.sendMessage("§c✗ Lieferung abgebrochen. Items zurückgegeben.");
            }
            return;
        }

        if (title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) {
            // Don't cleanup yet, player might be opening sign
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.hasMetadata(ORDER_SIGN_MODE)) {
                    return;
                }
                if (!player.getOpenInventory().getTitle().contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")) {
                    orderSystem.endCreateSession(player.getUniqueId());
                }
            }, 5L);
        }
    }

    // ==================== DELIVERY HELPERS ====================

    private void processDeliveryChest(Player player, Inventory chestInv) {
        OrderSystem.DeliverySession session = orderSystem.getDeliverySession(player.getUniqueId());
        if (session == null) return;

        OrderSystem.Order order = orderSystem.getOrders().stream()
            .filter(o -> o.id.equals(session.orderId))
            .findFirst()
            .orElse(null);

        if (order == null) {
            // Order no longer exists - return all items
            List<ItemStack> allItems = new ArrayList<>();
            for (int i = 9; i < chestInv.getSize(); i++) {
                ItemStack item = chestInv.getItem(i);
                if (item != null && item.getType() != Material.AIR) allItems.add(item);
            }
            giveItemsOrDrop(player, allItems);
            orderSystem.endDeliverySession(player.getUniqueId());
            player.sendMessage("§cOrder nicht mehr vorhanden! Items zurückgegeben.");
            return;
        }

        int needed = order.requiredAmount - order.delivered;
        List<ItemStack> matching = new ArrayList<>();
        List<ItemStack> toReturn = new ArrayList<>();

        // Collect and sort items from chest
        for (int i = 9; i < chestInv.getSize(); i++) {
            ItemStack item = chestInv.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;

            if (isShulkerBox(item.getType())) {
                extractFromShulker(item, order.itemType, matching);
                toReturn.add(item);
            } else if (item.isSimilar(order.itemType)) {
                matching.add(item);
            } else {
                toReturn.add(item);
            }
        }

        // Count matching items
        int matchCount = 0;
        for (ItemStack m : matching) matchCount += m.getAmount();

        if (matchCount == 0) {
            giveItemsOrDrop(player, toReturn);
            orderSystem.endDeliverySession(player.getUniqueId());
            player.sendMessage("§8§m                    ");
            player.sendMessage("§c✗ " + orderSystem.toSmallCaps("KEINE PASSENDEN ITEMS"));
            player.sendMessage("§8");
            player.sendMessage("§7Keine passenden Items gefunden.");
            player.sendMessage("§7Items wurden zurückgegeben.");
            player.sendMessage("§8§m                    ");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Cap at needed amount, return excess
        int deliverCount = Math.min(matchCount, needed);
        if (matchCount > needed) {
            capMatchingItems(matching, needed, toReturn);
        }

        // Return non-matching items + shulkers + excess
        giveItemsOrDrop(player, toReturn);

        // Store state for confirm
        session.matchingItems = matching;
        session.matchingCount = deliverCount;
        session.state = OrderSystem.DeliverySession.State.CONFIRMING;

        // Open confirm GUI
        double payment = deliverCount * order.pricePerItem;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.openInventory(orderSystem.createDeliveryConfirmGUI(player, deliverCount, payment));
        }, 2L);
    }

    private void extractFromShulker(ItemStack shulkerItem, ItemStack targetType, List<ItemStack> matching) {
        if (!(shulkerItem.getItemMeta() instanceof BlockStateMeta blockMeta)) return;
        if (!(blockMeta.getBlockState() instanceof ShulkerBox shulker)) return;

        Inventory shulkerInv = shulker.getInventory();
        for (int i = 0; i < shulkerInv.getSize(); i++) {
            ItemStack item = shulkerInv.getItem(i);
            if (item != null && item.isSimilar(targetType)) {
                matching.add(item.clone());
                shulkerInv.setItem(i, null);
            }
        }

        blockMeta.setBlockState(shulker);
        shulkerItem.setItemMeta(blockMeta);
    }

    private void capMatchingItems(List<ItemStack> matching, int maxCount, List<ItemStack> toReturn) {
        int total = 0;
        for (ItemStack m : matching) total += m.getAmount();

        if (total <= maxCount) return;

        int excess = total - maxCount;
        for (int i = matching.size() - 1; i >= 0 && excess > 0; i--) {
            ItemStack item = matching.get(i);
            if (item.getAmount() <= excess) {
                excess -= item.getAmount();
                matching.remove(i);
                toReturn.add(item);
            } else {
                ItemStack returnPart = item.clone();
                returnPart.setAmount(excess);
                toReturn.add(returnPart);
                item.setAmount(item.getAmount() - excess);
                excess = 0;
            }
        }
    }

    private void giveItemsOrDrop(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) continue;
            for (ItemStack drop : player.getInventory().addItem(item).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private boolean isShulkerBox(Material mat) {
        return mat.name().endsWith("SHULKER_BOX");
    }

    @EventHandler
    public void onSign(SignChangeEvent e) {
        Player player = e.getPlayer();
        String mode = getInputMode(player);
        if (mode == null) return;

        cleanupInputSign(player);

        String input = readInput(e);

        switch (mode) {
            case "search" -> handleBrowseSearch(player, input);
            case "item_search" -> handleItemSearch(player, input);
            case "amount" -> handleAmountInput(player, input);
            case "price" -> handlePriceInput(player, input);
        }
    }

    private void handleAmountInput(Player player, String input) {
        OrderSystem.CreateSession session = orderSystem.getCreateSession(player.getUniqueId());
        if (session == null) return;

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
                int amount = (int) NumberFormatter.parse(input.trim());

                if (amount <= 0) {
                    player.sendMessage("§cMenge muss größer als 0 sein! §7(z.B. 64, 1k)");
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
        }
    }

    private void handlePriceInput(Player player, String input) {
        OrderSystem.CreateSession session = orderSystem.getCreateSession(player.getUniqueId());
        if (session == null) return;

        if (input == null || input.trim().isEmpty()) {
            player.sendMessage("§cKeine Eingabe!");
            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId())));
            return;
        }

        try {
            double price = NumberFormatter.parse(input.trim().replace(",", "."));

            if (price <= 0) {
                player.sendMessage("§cPreis muss größer als 0 sein! §7(z.B. 100, 10k, 1.5m)");
                Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId())));
                return;
            }

            session.price = price;
            session.priceSet = true;

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId()));
                player.sendMessage("§a✓ Preis gesetzt: §e" + NumberFormatter.formatMoney(price) + "/Stück");
            });

        } catch (NumberFormatException ex) {
            player.sendMessage("§cUngültige Zahl!");
            Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(orderSystem.createNewOrderGUI(player.getUniqueId())));
        }
    }

    private void handleBrowseSearch(Player player, String input) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("stop") || trimmed.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int page = orderSystem.getBrowseSession(player.getUniqueId()).page;
                player.openInventory(orderSystem.createBrowseGUI(page, player.getUniqueId()));
                player.sendMessage("§8┃ §d§lORDER §8┃ §7Suche abgebrochen.");
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            orderSystem.setSearchQuery(player.getUniqueId(), trimmed);
            player.openInventory(orderSystem.createBrowseGUI(0, player.getUniqueId()));
            player.sendMessage("§8┃ §d§lORDER §8┃ §7Suche: §f\"" + trimmed + "\"");
        });
    }

    private void handleItemSearch(Player player, String input) {
        String trimmed = input == null ? "" : input.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("stop") || trimmed.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                int page = orderSystem.getBrowseSession(player.getUniqueId()).itemSelectPage;
                player.openInventory(orderSystem.createItemSelectGUI(player.getUniqueId(), page));
                player.sendMessage("§8┃ §d§lORDER §8┃ §7Suche abgebrochen.");
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            OrderSystem.BrowseSession session = orderSystem.getBrowseSession(player.getUniqueId());
            session.itemSelectSearch = trimmed;
            session.itemSelectPage = 0;
            player.openInventory(orderSystem.createItemSelectGUI(player.getUniqueId(), 0));
            player.sendMessage("§8┃ §d§lORDER §8┃ §7Suche: §f\"" + trimmed + "\"");
        });
    }

    private void openAmountSignGUI(Player player) {
        openInputSignGUI(player, "amount", "Menge", "z.B. 64 oder 1k", null);
    }

    private void openPriceSignGUI(Player player) {
        openInputSignGUI(player, "price", "Preis/Stück", "z.B. 250 oder 1.5k", null);
    }

    private void openInputSignGUI(Player player, String mode, String title, String hint, String initialValue) {
        try {
            cleanupInputSign(player);

            org.bukkit.block.Block block = player.getLocation().clone().add(0, 3, 0).getBlock();
            Material originalType = block.getType();

            player.setMetadata(ORDER_SIGN_MODE, new org.bukkit.metadata.FixedMetadataValue(plugin, mode));
            player.setMetadata(ORDER_SIGN_BLOCK, new org.bukkit.metadata.FixedMetadataValue(plugin, block.getLocation()));
            player.setMetadata(ORDER_SIGN_ORIGINAL, new org.bukkit.metadata.FixedMetadataValue(plugin, originalType.name()));

            block.setType(Material.OAK_SIGN);
            Sign sign = (Sign) block.getState();
            sign.setLine(0, initialValue == null ? "" : initialValue);
            sign.setLine(1, "^^^^^^^^^^^^^^");
            sign.setLine(2, title);
            sign.setLine(3, hint);
            sign.update(false, false);

            player.openSign(sign);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.hasMetadata(ORDER_SIGN_MODE)) {
                    cleanupInputSign(player);
                }
            }, 200L);

        } catch (Exception ex) {
            player.sendMessage("§cFehler beim Öffnen der Eingabe!");
        }
    }

    private String getInputMode(Player player) {
        return player.getMetadata(ORDER_SIGN_MODE).stream()
            .map(MetadataValue::asString)
            .findFirst()
            .orElse(null);
    }

    private void cleanupInputSign(Player player) {
        Location location = player.getMetadata(ORDER_SIGN_BLOCK).stream()
            .map(MetadataValue::value)
            .filter(Location.class::isInstance)
            .map(Location.class::cast)
            .findFirst()
            .orElse(null);
        String originalType = player.getMetadata(ORDER_SIGN_ORIGINAL).stream()
            .map(MetadataValue::asString)
            .findFirst()
            .orElse(null);
        if (location != null && originalType != null) {
            Material material = Material.matchMaterial(originalType);
            if (material != null) {
                location.getBlock().setType(material);
            }
        }
        player.removeMetadata(ORDER_SIGN_MODE, plugin);
        player.removeMetadata(ORDER_SIGN_BLOCK, plugin);
        player.removeMetadata(ORDER_SIGN_ORIGINAL, plugin);
    }

    private String readInput(SignChangeEvent event) {
        String line = event.getLine(0);
        return line == null ? "" : line.trim();
    }
}
