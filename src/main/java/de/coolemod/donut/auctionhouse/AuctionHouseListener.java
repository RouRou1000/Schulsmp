package de.coolemod.donut.auctionhouse;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
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
 * Event Handler für Auction House
 */
public class AuctionHouseListener implements Listener {
    private final DonutPlugin plugin;
    private final AuctionHouse ah;
    private int currentPage = 0;

    public AuctionHouseListener(DonutPlugin plugin, AuctionHouse ah) {
        this.plugin = plugin;
        this.ah = ah;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        String title = e.getView().getTitle();
        if (!ah.isAuctionGUI(title)) return;

        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        // Allow player inventory clicks
        if (slot >= e.getView().getTopInventory().getSize()) {
            // NEW AUCTION GUI: Allow taking items from inventory
            if (title.contains("ɴᴇᴜᴇ ᴀᴜᴋᴛɪᴏɴ")) {
                if (e.isShiftClick()) {
                    e.setCancelled(true);

                    AuctionHouse.CreateSession session = ah.getCreateSession(player.getUniqueId());
                    ItemStack targetSlot = e.getView().getTopInventory().getItem(4);
                    if (session == null || clicked == null || clicked.getType().isAir()) {
                        return;
                    }
                    if (targetSlot != null && !targetSlot.getType().isAir()) {
                        return;
                    }

                    ItemStack moved = clicked.clone();
                    e.getView().getTopInventory().setItem(4, moved);
                    e.getClickedInventory().setItem(e.getSlot(), null);
                    session.item = moved.clone();
                    session.priceSet = false;
                    player.updateInventory();
                } else {
                    e.setCancelled(false);
                }
            } else {
                e.setCancelled(true);
            }
            return;
        }

        // NEW AUCTION GUI: Slot 4 is completely free like a chest slot
        if (title.contains("ɴᴇᴜᴇ ᴀᴜᴋᴛɪᴏɴ") && slot == 4) {
            e.setCancelled(false);
            // Update session after inventory change
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack item = e.getView().getTopInventory().getItem(4);
                AuctionHouse.CreateSession session = ah.getCreateSession(player.getUniqueId());
                if (session != null) {
                    if (item != null && !item.getType().isAir()) {
                        session.item = item.clone();
                    } else {
                        session.item = null;
                        session.priceSet = false;
                    }
                }
            }, 1L);
            return;
        }

        // Cancel all other top inventory clicks
        e.setCancelled(true);

        String action = ah.getAction(clicked);
        if (action == null) {
            return;
        }

        // Handle actions
        switch (action) {
            case "border":
            case "disabled":
                // Do nothing
                break;

            case "buy":
                String auctionId = ah.getAuctionId(clicked);
                if (auctionId != null) {
                    if (ah.buyAuction(player, auctionId)) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            player.openInventory(ah.createBrowseGUI(currentPage, player.getUniqueId()));
                        }, 2L);
                    }
                }
                break;

            case "cancel":
                String cancelId = ah.getAuctionId(clicked);
                if (cancelId != null) {
                    ah.cancelAuction(player, cancelId);
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.openInventory(ah.createMyAuctionsGUI(player.getUniqueId()));
                    }, 2L);
                }
                break;

            case "prev":
                currentPage--;
                player.openInventory(ah.createBrowseGUI(currentPage, player.getUniqueId()));
                break;

            case "next":
                currentPage++;
                player.openInventory(ah.createBrowseGUI(currentPage, player.getUniqueId()));
                break;

            case "new":
                ah.startCreateSession(player.getUniqueId());
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(ah.createNewAuctionGUI(player.getUniqueId()));
                }, 2L);
                break;

            case "my_auctions":
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(ah.createMyAuctionsGUI(player.getUniqueId()));
                }, 2L);
                break;

            // ===== SEARCH & SORT ACTIONS =====
            case "search":
                if (e.isLeftClick()) {
                    openSearchSignGUI(player);
                } else if (e.isRightClick()) {
                    AuctionHouse.BrowseSession browseSession = ah.getBrowseSession(player.getUniqueId());
                    if (browseSession.searchQuery != null) {
                        browseSession.searchQuery = null;
                        player.sendMessage("§8┃ §e§lAH §8┃ §7Suche gelöscht.");
                        currentPage = 0;
                        player.openInventory(ah.createBrowseGUI(currentPage, player.getUniqueId()));
                    }
                }
                break;

            case "sort":
                AuctionHouse.BrowseSession sortSession = ah.getBrowseSession(player.getUniqueId());
                sortSession.sortMode = sortSession.sortMode.next();
                player.sendMessage("§8┃ §e§lAH §8┃ §7Sortierung: §e" + sortSession.sortMode.display);
                currentPage = 0;
                player.openInventory(ah.createBrowseGUI(currentPage, player.getUniqueId()));
                break;

            // ===== CREATE GUI ACTIONS =====
            case "back":
                ah.endCreateSession(player.getUniqueId());
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(ah.createBrowseGUI(0, player.getUniqueId()));
                }, 2L);
                break;

            case "set_price":
                AuctionHouse.CreateSession session = ah.getCreateSession(player.getUniqueId());
                if (session != null) {
                    openSignGUI(player);
                }
                break;

            case "confirm":
                AuctionHouse.CreateSession confirmSession = ah.getCreateSession(player.getUniqueId());
                if (confirmSession != null && confirmSession.priceSet && confirmSession.item != null) {
                    String id = ah.createAuction(player.getUniqueId(), confirmSession.item, confirmSession.price);
                    player.sendMessage("§8┃ §e§lAH §8┃ §aAuktion erstellt für §e" + NumberFormatter.formatMoney(confirmSession.price));
                    confirmSession.item = null; // Prevent item duplication
                    ah.endCreateSession(player.getUniqueId());
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.openInventory(ah.createBrowseGUI(0, player.getUniqueId()));
                    }, 2L);
                }
                break;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (ah.isAuctionGUI(title)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();

        String title = e.getView().getTitle();
        if (title.contains("ɴᴇᴜᴇ ᴀᴜᴋᴛɪᴏɴ")) {
            // Don't cleanup yet, player might be opening sign
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.getOpenInventory().getTitle().contains("ɴᴇᴜᴇ ᴀᴜᴋᴛɪᴏɴ")) {
                    ah.endCreateSession(player.getUniqueId());
                }
            }, 5L);
        }
    }

    @EventHandler
    public void onSign(SignChangeEvent e) {
        Player player = e.getPlayer();

        // Check for Create Session (Price Input)
        AuctionHouse.CreateSession session = ah.getCreateSession(player.getUniqueId());
        if (session != null) {
            handlePriceSign(e, player, session);
            return;
        }

        // Check for Browse Session (Search Input)
        AuctionHouse.BrowseSession browseSession = ah.getBrowseSession(player.getUniqueId());
        if (browseSession != null && player.hasMetadata("ah_search_mode")) {
            handleSearchSign(e, player, browseSession);
            return;
        }
    }

    private void handlePriceSign(SignChangeEvent e, Player player, AuctionHouse.CreateSession session) {
        String input = e.getLine(0);
        cleanupAhSign(player);
        if (input == null || input.trim().isEmpty()) {
            player.sendMessage("§8┃ §e§lAH §8┃ §7Keine Eingabe.");
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(ah.createNewAuctionGUI(player.getUniqueId()));
            });
            return;
        }

        try {
            double price = NumberFormatter.parse(input.trim().replace(",", "."));

            if (price <= 0) {
                player.sendMessage("§8┃ §e§lAH §8┃ §cPreis muss größer als 0 sein! §7(z.B. 1000, 10k, 1.5m)");
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.openInventory(ah.createNewAuctionGUI(player.getUniqueId()));
                });
                return;
            }

            session.price = price;
            session.priceSet = true;

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(ah.createNewAuctionGUI(player.getUniqueId()));
                player.sendMessage("§8┃ §e§lAH §8┃ §7Preis: §a" + NumberFormatter.formatMoney(price));
            });

        } catch (NumberFormatException ex) {
            player.sendMessage("§8┃ §e§lAH §8┃ §cUngültige Zahl!");
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(ah.createNewAuctionGUI(player.getUniqueId()));
            });
        }
    }

    private void handleSearchSign(SignChangeEvent e, Player player, AuctionHouse.BrowseSession browseSession) {
        String input = e.getLine(0);

        // Remove metadata and sign
        player.removeMetadata("ah_search_mode", plugin);
        cleanupAhSign(player);

        if (input == null || input.trim().isEmpty()) {
            player.sendMessage("§8┃ §e§lAH §8┃ §7Keine Eingabe.");
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(ah.createBrowseGUI(0, player.getUniqueId()));
            });
            return;
        }

        // Set search query
        browseSession.searchQuery = input.trim();
        currentPage = 0;

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.openInventory(ah.createBrowseGUI(0, player.getUniqueId()));
            player.sendMessage("§8┃ §e§lAH §8┃ §7Suche: §e\"" + input.trim() + "\"");
        });
    }

    private void openSearchSignGUI(Player player) {
        try {
            cleanupAhSign(player);
            player.setMetadata("ah_search_mode", new org.bukkit.metadata.FixedMetadataValue(plugin, true));

            org.bukkit.block.Block block = player.getLocation().add(0, 3, 0).getBlock();
            Material originalType = block.getType();

            player.setMetadata("ah_sign_block", new org.bukkit.metadata.FixedMetadataValue(plugin, block.getLocation()));
            player.setMetadata("ah_sign_original", new org.bukkit.metadata.FixedMetadataValue(plugin, originalType.name()));

            block.setType(Material.OAK_SIGN, false);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setWaxed(false);
            org.bukkit.block.sign.SignSide front = sign.getSide(org.bukkit.block.sign.Side.FRONT);
            front.setLine(0, "");
            front.setLine(1, "^^^^^^^^^^^^^^");
            front.setLine(2, "Item-Name suchen");
            front.setLine(3, "");
            sign.update(true, false);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() == Material.OAK_SIGN) {
                    player.openSign((org.bukkit.block.Sign) block.getState(), org.bukkit.block.sign.Side.FRONT);
                }
            }, 3L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cleanupAhSign(player);
                player.removeMetadata("ah_search_mode", plugin);
            }, 200L);

        } catch (Exception ex) {
            player.sendMessage("§8┃ §e§lAH §8┃ §cFehler beim Öffnen der Eingabe!");
            cleanupAhSign(player);
            player.removeMetadata("ah_search_mode", plugin);
        }
    }

    private void openSignGUI(Player player) {
        try {
            cleanupAhSign(player);

            org.bukkit.block.Block block = player.getLocation().add(0, 3, 0).getBlock();
            Material originalType = block.getType();

            player.setMetadata("ah_sign_block", new org.bukkit.metadata.FixedMetadataValue(plugin, block.getLocation()));
            player.setMetadata("ah_sign_original", new org.bukkit.metadata.FixedMetadataValue(plugin, originalType.name()));

            block.setType(Material.OAK_SIGN, false);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setWaxed(false);
            org.bukkit.block.sign.SignSide front = sign.getSide(org.bukkit.block.sign.Side.FRONT);
            front.setLine(0, "");
            front.setLine(1, "^^^^^^^^^^^^^^");
            front.setLine(2, "Preis eingeben");
            front.setLine(3, "");
            sign.update(true, false);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() == Material.OAK_SIGN) {
                    player.openSign((org.bukkit.block.Sign) block.getState(), org.bukkit.block.sign.Side.FRONT);
                }
            }, 3L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                cleanupAhSign(player);
            }, 200L);

        } catch (Exception ex) {
            player.sendMessage("§8┃ §e§lAH §8┃ §cFehler beim Öffnen der Eingabe!");
            cleanupAhSign(player);
        }
    }

    private void cleanupAhSign(Player player) {
        org.bukkit.Location location = player.getMetadata("ah_sign_block").stream()
            .map(org.bukkit.metadata.MetadataValue::value)
            .filter(org.bukkit.Location.class::isInstance)
            .map(org.bukkit.Location.class::cast)
            .findFirst()
            .orElse(null);
        String originalType = player.getMetadata("ah_sign_original").stream()
            .map(org.bukkit.metadata.MetadataValue::asString)
            .findFirst()
            .orElse(null);
        if (location != null && originalType != null) {
            Material material = Material.matchMaterial(originalType);
            if (material != null) {
                location.getBlock().setType(material);
            }
        }
        player.removeMetadata("ah_sign_block", plugin);
        player.removeMetadata("ah_sign_original", plugin);
    }
}
