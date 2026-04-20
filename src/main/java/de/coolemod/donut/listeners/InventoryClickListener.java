package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
// import de.coolemod.donut.gui.AuctionGUI; // OLD - Now using AuctionEventHandler
import de.coolemod.donut.gui.OrdersGUI;
import de.coolemod.donut.gui.SlayShopGUI;
import de.coolemod.donut.gui.ShopGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import de.coolemod.donut.managers.OrdersManager;

/**
 * Behandelt Klicks in den polierten GUIs (SlayShop, Shop, Auction, Orders)
 */
public class InventoryClickListener implements Listener {
    private final DonutPlugin plugin;

    public InventoryClickListener(DonutPlugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor = e.getCursor();

        // BLOCKIERE GLASS PANES KOMPLETT -egal welcher Click-Type oder Position
        if (clicked != null && clicked.getType().name().contains("STAINED_GLASS_PANE")) {
            // Prüfe ob im GUI geklickt wurde
            if (e.getRawSlot() < e.getView().getTopInventory().getSize()) {
                // NUR erlauben wenn PDC Action-Key vorhanden
                boolean hasActionKey = false;
                if (clicked.hasItemMeta()) {
                    org.bukkit.inventory.meta.ItemMeta meta = clicked.getItemMeta();
                    org.bukkit.NamespacedKey key1 = new org.bukkit.NamespacedKey(plugin, "donut_gui_action");
                    org.bukkit.NamespacedKey key2 = new org.bukkit.NamespacedKey(plugin, "ah_action");

                    if (meta.getPersistentDataContainer().has(key1, org.bukkit.persistence.PersistentDataType.STRING) ||
                        meta.getPersistentDataContainer().has(key2, org.bukkit.persistence.PersistentDataType.STRING)) {
                        hasActionKey = true;
                    }
                }

                if (!hasActionKey) {
                    e.setCancelled(true);
                    e.setResult(org.bukkit.event.Event.Result.DENY);
                    e.setCursor(null); // Entferne Item vom Cursor
                    return;
                }
            }
        }

        // Blockiere auch wenn Glass Pane auf Cursor ist (wird gerade bewegt)
        if (cursor != null && cursor.getType().name().contains("STAINED_GLASS_PANE")) {
            if (cursor.hasItemMeta()) {
                org.bukkit.inventory.meta.ItemMeta meta = cursor.getItemMeta();
                org.bukkit.NamespacedKey key1 = new org.bukkit.NamespacedKey(plugin, "donut_gui_action");
                org.bukkit.NamespacedKey key2 = new org.bukkit.NamespacedKey(plugin, "ah_action");

                // Wenn KEINE Action-Keys → Blockieren
                if (!meta.getPersistentDataContainer().has(key1, org.bukkit.persistence.PersistentDataType.STRING) &&
                    !meta.getPersistentDataContainer().has(key2, org.bukkit.persistence.PersistentDataType.STRING)) {
                    e.setCancelled(true);
                    e.setResult(org.bukkit.event.Event.Result.DENY);
                    e.setCursor(null);
                    return;
                }
            } else {
                // Glass Pane ohne Meta = Border Item → BLOCKIEREN
                e.setCancelled(true);
                e.setResult(org.bukkit.event.Event.Result.DENY);
                e.setCursor(null);
                return;
            }
        }

        if (clicked == null) return;

        // Worth GUI: komplett blockieren + Navigation
        if (title.contains("ɪᴛᴇᴍ ᴘʀɪᴄᴇs") || title.contains("ITEM PRICES")) {
            e.setCancelled(true);
            e.setResult(org.bukkit.event.Event.Result.DENY);
            if (clicked == null || !clicked.hasItemMeta()) return;
            org.bukkit.entity.Player wp = (org.bukkit.entity.Player) e.getWhoClicked();
            // Seiten-Navigation über worth_page PDC
            NamespacedKey worthPageKey = new NamespacedKey(plugin, "worth_page");
            if (clicked.getItemMeta().getPersistentDataContainer().has(worthPageKey, PersistentDataType.INTEGER)) {
                int page = clicked.getItemMeta().getPersistentDataContainer().get(worthPageKey, PersistentDataType.INTEGER);
                wp.playSound(wp.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                new de.coolemod.donut.gui.WorthGUI(plugin).open(wp, page);
                return;
            }
            // Verzauberungs-Tab Button
            NamespacedKey tabKeyW = new NamespacedKey(plugin, "worth_tab");
            if (clicked.getItemMeta().getPersistentDataContainer().has(tabKeyW, PersistentDataType.STRING)) {
                String tab = clicked.getItemMeta().getPersistentDataContainer().get(tabKeyW, PersistentDataType.STRING);
                if ("enchants".equals(tab)) {
                    wp.playSound(wp.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                    new de.coolemod.donut.gui.WorthGUI(plugin).openEnchants(wp, 0);
                }
                return;
            }
            if (clicked.getType() == Material.COMPASS) {
                if (e.isRightClick() && de.coolemod.donut.gui.WorthGUI.getSearchQuery(wp.getUniqueId()) != null) {
                    de.coolemod.donut.gui.WorthGUI.clearSearchQuery(wp.getUniqueId());
                    wp.playSound(wp.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                    new de.coolemod.donut.gui.WorthGUI(plugin).open(wp, 0);
                    wp.sendMessage("§8┃ §e§lWORTH §8┃ §7Suche gelöscht.");
                } else {
                    wp.closeInventory();
                    PlayerChatListener.openWorthSearchSign(plugin, wp);
                }
                return;
            }
            // Schließen-Button
            if (clicked.getType() == Material.BARRIER) {
                wp.closeInventory();
                return;
            }
            return;
        }

        // Settings GUI: komplett blockieren + Toggle
        if (de.coolemod.donut.gui.SettingsGUI.isSettingsGUI(title)) {
            e.setCancelled(true);
            e.setResult(org.bukkit.event.Event.Result.DENY);
            if (clicked == null || !clicked.hasItemMeta()) return;
            org.bukkit.entity.Player sp = (org.bukkit.entity.Player) e.getWhoClicked();
            new de.coolemod.donut.gui.SettingsGUI(plugin).handleClick(sp, clicked);
            return;
        }

        // Enchant Worth GUI: komplett blockieren + Navigation
        if (title.contains("ᴇɴᴄʜᴀɴᴛ ᴘʀɪᴄᴇs")) {
            e.setCancelled(true);
            e.setResult(org.bukkit.event.Event.Result.DENY);
            if (clicked == null || !clicked.hasItemMeta()) return;
            org.bukkit.entity.Player wp = (org.bukkit.entity.Player) e.getWhoClicked();
            // Seiten-Navigation
            NamespacedKey enchPageKey = new NamespacedKey(plugin, "worth_page");
            if (clicked.getItemMeta().getPersistentDataContainer().has(enchPageKey, PersistentDataType.INTEGER)) {
                int page = clicked.getItemMeta().getPersistentDataContainer().get(enchPageKey, PersistentDataType.INTEGER);
                wp.playSound(wp.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                new de.coolemod.donut.gui.WorthGUI(plugin).openEnchants(wp, page);
                return;
            }
            // Zurück zu Items
            NamespacedKey tabKey = new NamespacedKey(plugin, "worth_tab");
            if (clicked.getItemMeta().getPersistentDataContainer().has(tabKey, PersistentDataType.STRING)) {
                String tab = clicked.getItemMeta().getPersistentDataContainer().get(tabKey, PersistentDataType.STRING);
                if ("items".equals(tab)) {
                    wp.playSound(wp.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                    new de.coolemod.donut.gui.WorthGUI(plugin).open(wp, 0);
                }
                return;
            }
            if (clicked.getType() == Material.BARRIER) {
                wp.closeInventory();
                return;
            }
            return;
        }

        // KRITISCH: Blockiere ALLE Shop-GUIs SOFORT und KOMPLETT
        if (title.contains("Slay Shop") || title.contains("SHOP") || title.contains("SCHUL") || title.contains("FOOD") || title.contains("GEAR") || title.contains("Wähle dein Gear") || title.contains("NETHER") || title.contains("SHARDS") || title.contains("ᴀᴜᴋᴛɪᴏɴѕʜᴀᴜѕ") || title.contains("AUKTIONSHAUS") || title.contains("Orders") || title.contains("Kiste") || title.contains("KISTE") || title.contains("Wähle dein Gear") || title.contains("DONUT CORE") || title.contains("ᴍᴇɪɴᴇ ᴀᴜᴋᴛɪᴏɴᴇɴ") || title.contains("MEINE AUKTIONEN") || title.contains("Hilfe") || title.contains("SELL MULTI") || title.contains("ᴄᴏʟʟᴇᴄᴛ ɪᴛᴇᴍs") || title.contains("ᴏʀᴅᴇʀ ᴅᴇᴛᴀɪʟs") || title.contains("ᴏʀᴅᴇʀ sᴛᴏʀɴɪᴇʀᴇɴ") || title.contains("ɪᴛᴇᴍs ᴀʙʜᴏʟᴇɴ") || title.contains("Collect Items") || title.contains("ʙᴀʟᴛᴏᴘ")) {
            e.setCancelled(true);
            e.setResult(org.bukkit.event.Event.Result.DENY);
        }

        // Baltop GUI: Seitennavigation
        if (title.contains("ʙᴀʟᴛᴏᴘ")) {
            e.setCancelled(true);
            e.setResult(org.bukkit.event.Event.Result.DENY);
            if (clicked == null || !clicked.hasItemMeta()) return;
            org.bukkit.entity.Player bp = (org.bukkit.entity.Player) e.getWhoClicked();

            if (e.getSlot() == 45 && clicked.getType() == Material.ARROW) {
                // Vorherige Seite
                String t = title;
                int currentPage = 0;
                try {
                    int openP = t.indexOf('(');
                    int slash = t.indexOf('/');
                    if (openP >= 0 && slash > openP) {
                        currentPage = Integer.parseInt(t.substring(openP + 1, slash).trim()) - 1;
                    }
                } catch (Exception ignored) {}
                bp.playSound(bp.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                bp.openInventory(new de.coolemod.donut.commands.BaltopCommand(plugin).createBaltopGUI(bp.getUniqueId(), currentPage - 1));
                return;
            }
            if (e.getSlot() == 53 && clicked.getType() == Material.ARROW) {
                // Nächste Seite
                String t = title;
                int currentPage = 0;
                try {
                    int openP = t.indexOf('(');
                    int slash = t.indexOf('/');
                    if (openP >= 0 && slash > openP) {
                        currentPage = Integer.parseInt(t.substring(openP + 1, slash).trim()) - 1;
                    }
                } catch (Exception ignored) {}
                bp.playSound(bp.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                bp.openInventory(new de.coolemod.donut.commands.BaltopCommand(plugin).createBaltopGUI(bp.getUniqueId(), currentPage + 1));
                return;
            }
            return;
        }

        // Sell-GUI: Erlaube Item-Platzierung, aber blockiere Buttons
        if (title.contains("ɪᴛᴇᴍѕ ᴠᴇʀᴋᴀᴜꜰᴇɴ") || title.contains("ITEMS VERKAUFEN")) {
            ItemStack current = e.getCurrentItem();
            int slot = e.getRawSlot();

            // Wenn im Top-Inventory (GUI)
            if (slot < 54) {
                // Prüfe ob es ein Button ist - aber blockiere den Click NICHT mit return!
                if (current != null && current.hasItemMeta() && current.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING)) {
                    e.setCancelled(true);
                    // KEIN RETURN - lasse Code weiterlaufen zu Action-Handlern
                } else if (current != null && (current.getType() == Material.BLACK_STAINED_GLASS_PANE || current.getType() == Material.GRAY_STAINED_GLASS_PANE)) {
                    e.setCancelled(true);
                    return;
                } else if (current != null && (current.getType() == Material.WRITABLE_BOOK || current.getType() == Material.EMERALD || current.getType() == Material.BARRIER || current.getType() == Material.ARROW || current.getType() == Material.SUNFLOWER || current.getType() == Material.GOLD_INGOT || current.getType() == Material.LIME_STAINED_GLASS_PANE)) {
                    e.setCancelled(true); // Info/Worth/Navigation Items
                    return;
                } else if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
                    e.setCancelled(true);
                    return;
                }
                // Sonst: Erlaube Item-Interaktion in den freien Slots
            }

            // Update Worth nach Klick
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (e.getView().getTitle().contains("ɪᴛᴇᴍѕ ᴠᴇʀᴋᴀᴜꜰᴇɴ") || e.getView().getTitle().contains("ITEMS VERKAUFEN")) {
                    de.coolemod.donut.gui.SellGUI.updateWorthDisplay(plugin, e.getView().getTopInventory());
                }
            }, 1L);
        }

        // AuctionCreate-GUI: Erlaube Item-Platzierung, aber blockiere Buttons
        if (title.contains("ᴀᴜᴋᴛɪᴏɴ ᴇʀѕᴛᴇʟʟᴇɴ") || title.contains("AUKTION ERSTELLEN")) {
            ItemStack current = e.getCurrentItem();
            int slot = e.getRawSlot();

            // Wenn im Top-Inventory (GUI)
            if (slot < 54) {
                // Prüfe zuerst ob es ein Button mit AH Action ist (ah_action Key)
                if (current != null && current.hasItemMeta() && current.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "ah_action"), PersistentDataType.STRING)) {
                    e.setCancelled(true);
                    // KEIN return - lasse Code zu Action-Handlern weiterlaufen
                }
                // Blockiere alle Glass Panes OHNE Action-Key (inkl. grauer Confirm-Button)
                else if (current != null && current.getType().name().contains("GLASS_PANE")) {
                    e.setCancelled(true);
                    return;
                } else if (current != null && current.getType() == Material.WRITABLE_BOOK) {
                    e.setCancelled(true); // Info Item
                    return;
                } else if (current != null && (current.getType() == Material.ARROW || current.getType() == Material.BARRIER || current.getType() == Material.EMERALD || current.getType() == Material.GOLD_INGOT)) {
                    e.setCancelled(true); // Navigation/Action Items
                    return;
                } else if (slot < 9 || slot >= 45 || slot % 9 == 0 || slot % 9 == 8) {
                    e.setCancelled(true);
                    return;
                }
                // Sonst: Erlaube Item-Interaktion in den freien Slots (10-43)
            }

            // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
            // Update GUI nach Item-Änderung
            // org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            //     if (e.getView().getTitle().contains("ᴀᴜᴋᴛɪᴏɴ ᴇʀѕᴛᴇʟʟᴇɴ") || e.getView().getTitle().contains("AUKTION ERSTELLEN")) {
            //         org.bukkit.entity.Player p = (org.bukkit.entity.Player)e.getWhoClicked();
            //         new de.coolemod.donut.gui.AuctionCreateGUI(plugin).open(p);
            //     }
            // }, 1L);
        }

        // Prüfe AH-Aktionen
        NamespacedKey ahKey = new NamespacedKey(plugin, "ah_action");
        if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(ahKey, PersistentDataType.STRING)) {
            String ahAction = clicked.getItemMeta().getPersistentDataContainer().get(ahKey, PersistentDataType.STRING);
            org.bukkit.Bukkit.getLogger().info("AH Action: " + ahAction);
            switch (ahAction) {
                // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
                case "setprice":
                    // org.bukkit.entity.Player p_price = (org.bukkit.entity.Player)e.getWhoClicked();
                    // org.bukkit.Bukkit.getLogger().info("Opening price input for " + p_price.getName());
                    //
                    // // Speichere Items aus der GUI
                    // de.coolemod.donut.gui.AuctionCreateGUI.saveItems(p_price, e.getView().getTopInventory());
                    //
                    // // Markiere Spieler als in Preis-Eingabe (damit Items nicht zurückgegeben werden)
                    // p_price.setMetadata("ah_setting_price", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    //
                    // // Schließe GUI erst, dann öffne Sign im nächsten Tick
                    // p_price.closeInventory();
                    // org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    //     try {
                    //         new de.coolemod.donut.gui.AnvilInputGUI(plugin).openPriceInput(p_price);
                    //     } catch (Exception ex) {
                    //         org.bukkit.Bukkit.getLogger().severe("Error in setprice: " + ex.getMessage());
                    //         ex.printStackTrace();
                    //         p_price.sendMessage("§cFehler beim Öffnen der Preis-Eingabe!");
                    //         p_price.removeMetadata("ah_setting_price", plugin);
                    //     }
                    // }, 2L);
                    return;
                // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
                case "confirm":
                    // org.bukkit.entity.Player p_confirm = (org.bukkit.entity.Player)e.getWhoClicked();
                    // org.bukkit.inventory.Inventory createInv = e.getView().getTopInventory();
                    //
                    // // Prüfe Preis aus HashMap
                    // double price = de.coolemod.donut.gui.AuctionCreateGUI.getPrice(p_confirm);
                    // if (price <= 0) {
                    //     p_confirm.sendMessage("§cKein Preis gesetzt!");
                    //     return;
                    // }
                    //
                    // // Sammle Items
                    // java.util.List<ItemStack> auctionItems = new java.util.ArrayList<>();
                    //     if (i % 9 == 0 || i % 9 == 8) continue;
                    //     ItemStack aItem = createInv.getItem(i);
                    //     if (aItem == null || aItem.getType() == Material.AIR) continue;
                    //     if (aItem.hasItemMeta() && aItem.getItemMeta().getPersistentDataContainer().has(ahKey, PersistentDataType.STRING)) continue;
                    //     // Blockiere ALLE Glass Panes (nicht nur Black/Gray)
                    //     if (aItem.getType().name().contains("GLASS_PANE")) continue;
                    //     // Blockiere andere GUI-Elemente
                    //     if (aItem.getType() == Material.WRITABLE_BOOK || aItem.getType() == Material.ARROW ||
                    //         aItem.getType() == Material.GOLD_INGOT || aItem.getType() == Material.BARRIER) continue;
                    //     auctionItems.add(aItem.clone());
                    // }
                    //
                    // if (auctionItems.isEmpty()) {
                    //     p_confirm.sendMessage("§cKeine Items in der Auktion!");
                    //     return;
                    // }
                    //
                    // // Erstelle Auktionen
                    // for (ItemStack auctionItem : auctionItems) {
                    //     plugin.getAuctionManager().listItem(p_confirm.getUniqueId(), auctionItem, price);
                    // }
                    //
                    // // Lösche Items aus GUI
                    // for (int i = 10; i <= 43; i++) {
                    //     if (i % 9 == 0 || i % 9 == 8) continue;
                    //     ItemStack delItem = createInv.getItem(i);
                    //     if (delItem == null || delItem.getType() == Material.AIR) continue;
                    //     if (delItem.hasItemMeta() && delItem.getItemMeta().getPersistentDataContainer().has(ahKey, PersistentDataType.STRING)) continue;
                    //     // Blockiere ALLE Glass Panes (nicht nur Black/Gray)
                    //     if (delItem.getType().name().contains("GLASS_PANE")) continue;
                    //     // Blockiere andere GUI-Elemente
                    //     if (delItem.getType() == Material.WRITABLE_BOOK || delItem.getType() == Material.ARROW ||
                    //         delItem.getType() == Material.GOLD_INGOT || delItem.getType() == Material.BARRIER) continue;
                    //     createInv.setItem(i, null);
                    // }
                    //
                    // p_confirm.closeInventory();
                    // // WICHTIG: Lösche gespeicherte Items + Preis aus HashMap (verhindert Dupe!)
                    // de.coolemod.donut.gui.AuctionCreateGUI.clearPrice(p_confirm);
                    // de.coolemod.donut.gui.AuctionCreateGUI.clearItems(p_confirm);
                    // p_confirm.sendMessage("§a✓ " + auctionItems.size() + " Auktion(en) erstellt für je $" + "%.2f".formatted(price) + "!");
                    // p_confirm.playSound(p_confirm.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    return;
                // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
                case "back":
                    // org.bukkit.entity.Player p_back = (org.bukkit.entity.Player)e.getWhoClicked();
                    // p_back.closeInventory();
                    // new AuctionGUI(plugin).open(p_back, 1);
                    return;
            }
        }

        // Prüfe PDC-Aktionen ZUERST (bevor Crate-Cancel)
        NamespacedKey actionKey = new NamespacedKey(plugin, "donut_gui_action");
        NamespacedKey pageKey = new NamespacedKey(plugin, "donut_gui_page");
        ItemStack is = clicked;
        if (is.hasItemMeta() && is.getItemMeta().getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            String action = is.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
            Integer page = is.getItemMeta().getPersistentDataContainer().has(pageKey, PersistentDataType.INTEGER) ? is.getItemMeta().getPersistentDataContainer().get(pageKey, PersistentDataType.INTEGER) : 1;
            switch (action) {
                // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
                case "auction_prev":
                case "auction_next":
                    // new AuctionGUI(plugin).open((org.bukkit.entity.Player)e.getWhoClicked(), page);
                    return;
                case "auction_close":
                    // ((org.bukkit.entity.Player)e.getWhoClicked()).closeInventory();
                    return;
                case "auction_my":
                    // new AuctionGUI(plugin).openMyAuctions((org.bukkit.entity.Player)e.getWhoClicked());
                    return;
                case "auction_back":
                    // new AuctionGUI(plugin).open((org.bukkit.entity.Player)e.getWhoClicked(), 1);
                    return;
                case "auction_browse":
                    // new AuctionGUI(plugin).open((org.bukkit.entity.Player)e.getWhoClicked(), 1);
                    return;
                // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
                case "auction_create_close":
                    // org.bukkit.entity.Player p_close = (org.bukkit.entity.Player)e.getWhoClicked();
                    // org.bukkit.inventory.Inventory closeInv = e.getView().getTopInventory();
                    //
                    // // Gib alle Items zurück
                    // for (int i = 10; i <= 43; i++) {
                    //     if (i % 9 == 0 || i % 9 == 8) continue;
                    //     ItemStack returnItem = closeInv.getItem(i);
                    //     if (returnItem == null || returnItem.getType() == Material.AIR) continue;
                    //     if (returnItem.hasItemMeta() && returnItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING)) {
                    //         continue;
                    //     }
                    //     if (returnItem.getType() == Material.BLACK_STAINED_GLASS_PANE || returnItem.getType() == Material.GRAY_STAINED_GLASS_PANE) continue;
                    //
                    //     // Gib Item zurück
                    //     java.util.HashMap<Integer, ItemStack> leftover = p_close.getInventory().addItem(returnItem);
                    //     if (!leftover.isEmpty()) {
                    //         for (ItemStack left : leftover.values()) {
                    //             p_close.getWorld().dropItemNaturally(p_close.getLocation(), left);
                    //         }
                    //     }
                    // }
                    //
                    // p_close.closeInventory();
                    return;
                // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
                case "auction_create":
                    // // Öffne Create-GUI für neue Auktion
                    // org.bukkit.entity.Player pl_create = (org.bukkit.entity.Player)e.getWhoClicked();
                    // new de.coolemod.donut.gui.AuctionCreateGUI(plugin).open(pl_create);
                    return;
                case "orders_prev":
                case "orders_next":
                    new OrdersGUI(plugin).open((org.bukkit.entity.Player)e.getWhoClicked(), page);
                    return;
                case "orders_close":
                    ((org.bukkit.entity.Player)e.getWhoClicked()).closeInventory();
                    return;
                case "orders_my":
                    org.bukkit.entity.Player ordersMyPlayer = (org.bukkit.entity.Player)e.getWhoClicked();
                    ordersMyPlayer.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        ordersMyPlayer.openInventory(plugin.getOrderSystem().createMyOrdersGUI(ordersMyPlayer.getUniqueId()));
                    }, 2L);
                    return;
                case "orders_back":
                    new OrdersGUI(plugin).open((org.bukkit.entity.Player)e.getWhoClicked(), 1);
                    return;
                case "orders_create":
                    org.bukkit.entity.Player pl2 = (org.bukkit.entity.Player)e.getWhoClicked();
                    pl2.closeInventory();
                    pl2.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§eSchreibe im Chat: §forder <Menge> <Preis/Stück>");
                    pl2.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§7(Halte ein Item in der Hand)");
                    pl2.setMetadata("order_create_pending", new org.bukkit.metadata.FixedMetadataValue(plugin, System.currentTimeMillis()));
                    return;
                default:
                    // Support für crate actions: crate_open:<id>, crate_buy:<id>
                    if (action.startsWith("crate_open:")) {
                        String id = action.split(":" , 2)[1];
                        org.bukkit.entity.Player p3 = (org.bukkit.entity.Player)e.getWhoClicked();
                        plugin.getCrateManager().openCrateSelection(p3, id, false);
                        return;
                    } else if (action.startsWith("crate_select_pool:")) {
                        String[] parts = action.split(":", 3);
                        if (parts.length == 3) {
                            String id = parts[1];
                            int poolIdx = Integer.parseInt(parts[2]);
                            org.bukkit.entity.Player pSel = (org.bukkit.entity.Player)e.getWhoClicked();
                            plugin.getCrateManager().giveSelectionRewards(pSel, id, poolIdx);
                        }
                        return;
                    } else if (action.equals("crate_selection_border")) {
                        return;
                    } else if (action.startsWith("crate_buy:")) {
                        // ENTFERNT: Kein Kauf mit Geld mehr möglich
                        return;
                    } else if (action.startsWith("crate_buy_money:")) {
                        // ENTFERNT: Kein Kauf mit Geld mehr möglich
                        return;
                    } else if (action.startsWith("crate_buy_shards:")) {
                        String id = action.split(":" , 2)[1];
                        org.bukkit.entity.Player p4c = (org.bukkit.entity.Player)e.getWhoClicked();
                        int keyCost = plugin.getConfig().getInt("settings.key-price-shards." + id, 50);
                        if (plugin.getShards().removeShards(p4c.getUniqueId(), keyCost)) {
                            plugin.getCrateManager().giveKeys(p4c.getUniqueId(), id, 1);
                            p4c.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Du hast einen §e" + id + "-Schlüssel §afür §d" + keyCost + " Shards §agekauft!");
                            p4c.playSound(p4c.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                            new de.coolemod.donut.gui.CrateDetailGUI(plugin).open(p4c, id);
                        } else {
                            p4c.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Nicht genug Shards! Benötigt: §d" + keyCost);
                            p4c.playSound(p4c.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        }
                        return;
                    } else if (action.startsWith("crate_test:")) {
                        String id = action.split(":" , 2)[1];
                        org.bukkit.entity.Player p5 = (org.bukkit.entity.Player)e.getWhoClicked();
                        if (!p5.hasPermission("donut.crate.admin")) { p5.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Keine Berechtigung."); return; }
                        plugin.getCrateManager().openCrateSelection(p5, id, true);
                        p5.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Admin-Test: Kiste geöffnet.");
                        return;
                    } else if (action.equals("crate_back")) {
                        org.bukkit.entity.Player p6 = (org.bukkit.entity.Player)e.getWhoClicked();
                        new de.coolemod.donut.gui.CrateGUI(plugin).open(p6);
                        return;
                    } else if (action.equals("open_shop")) {
                        org.bukkit.entity.Player p7 = (org.bukkit.entity.Player)e.getWhoClicked();
                        new de.coolemod.donut.gui.ShopGUI(plugin).open(p7);
                        return;
                    } else if (action.equals("open_slayshop")) {
                        org.bukkit.entity.Player p8 = (org.bukkit.entity.Player)e.getWhoClicked();
                        new de.coolemod.donut.gui.SlayShopGUI(plugin).open(p8);
                        return;
                    } else if (action.equals("open_auction")) {
                        // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
                        // org.bukkit.entity.Player p9 = (org.bukkit.entity.Player)e.getWhoClicked();
                        // new de.coolemod.donut.gui.AuctionCreateGUI(plugin).open(p9);
                        // Use /ah create instead
                        return;
                    } else if (action.equals("open_orders")) {
                        org.bukkit.entity.Player p10 = (org.bukkit.entity.Player)e.getWhoClicked();
                        p10.openInventory(plugin.getOrderSystem().createBrowseGUI(0, p10.getUniqueId()));
                        return;
                    } else if (action.equals("open_crates")) {
                        org.bukkit.entity.Player p11 = (org.bukkit.entity.Player)e.getWhoClicked();
                        new de.coolemod.donut.gui.CrateGUI(plugin).open(p11);
                        return;
                    } else if (action.equals("run_sell")) {
                        org.bukkit.entity.Player p12 = (org.bukkit.entity.Player)e.getWhoClicked();
                        p12.closeInventory();
                        new de.coolemod.donut.gui.SellGUI(plugin).open(p12);
                        return;
                    } else if (action.equals("open_sell")) {
                        org.bukkit.entity.Player p12b = (org.bukkit.entity.Player)e.getWhoClicked();
                        new de.coolemod.donut.gui.SellGUI(plugin).open(p12b);
                        return;
                    } else if (action.equals("run_worth")) {
                        org.bukkit.entity.Player p13 = (org.bukkit.entity.Player)e.getWhoClicked();
                        new de.coolemod.donut.gui.WorthGUI(plugin).open(p13, 0);
                        return;
                    } else if (action.equals("close_menu")) {
                        org.bukkit.entity.Player p14 = (org.bukkit.entity.Player)e.getWhoClicked();
                        p14.closeInventory();
                        return;
                    } else if (action.equals("sell_items")) {
                        org.bukkit.entity.Player p15 = (org.bukkit.entity.Player)e.getWhoClicked();
                        org.bukkit.inventory.Inventory sellInv = e.getView().getTopInventory();

                        // Sammle alle Items aus den Sell-Slots (inkl. Shulker-Inhalt)
                        double total = 0.0;
                        int itemsSold = 0;
                        java.util.List<ItemStack> itemsToSell = new java.util.ArrayList<>();

                        for (int i = 10; i <= 43; i++) {
                            if (i % 9 == 0 || i % 9 == 8) continue;
                            ItemStack sellItem = sellInv.getItem(i);
                            if (sellItem == null || sellItem.getType() == Material.AIR) continue;
                            if (sellItem.hasItemMeta() && sellItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING)) {
                                continue;
                            }
                            // Shulker Box: verkaufe Inhalt, oder leere Shulker als Item
                            if (sellItem.getType().name().endsWith("SHULKER_BOX")) {
                                boolean hadContent = false;
                                if (sellItem.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta blockMeta
                                        && blockMeta.getBlockState() instanceof org.bukkit.block.ShulkerBox box) {
                                    org.bukkit.inventory.Inventory shulkerInv = box.getInventory();
                                    for (int j = 0; j < shulkerInv.getSize(); j++) {
                                        ItemStack si = shulkerInv.getItem(j);
                                        if (si == null || si.getType() == Material.AIR) continue;
                                        double w = plugin.getWorthManager().getWorth(si);
                                        if (w > 0) {
                                            total += w * si.getAmount();
                                            itemsSold += si.getAmount();
                                            itemsToSell.add(si.clone());
                                            shulkerInv.setItem(j, null);
                                            hadContent = true;
                                        }
                                    }
                                    if (hadContent) {
                                        blockMeta.setBlockState(box);
                                        sellItem.setItemMeta(blockMeta);
                                        continue;
                                    }
                                }
                                // Leere Shulker: als Item verkaufen
                                double shulkerWorth = plugin.getWorthManager().getWorth(sellItem);
                                if (shulkerWorth > 0) {
                                    total += shulkerWorth * sellItem.getAmount();
                                    itemsSold += sellItem.getAmount();
                                    itemsToSell.add(sellItem);
                                }
                                continue;
                            }
                            double worth = plugin.getWorthManager().getWorth(sellItem);
                            if (worth > 0) {
                                total += worth * sellItem.getAmount();
                                itemsSold += sellItem.getAmount();
                                itemsToSell.add(sellItem);
                            }
                        }

                        if (total <= 0) {
                            p15.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Keine verkaufbaren Items!");
                            p15.playSound(p15.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            return;
                        }

                        // Entferne verkaufte Items (inkl. leere Shulkers)
                        for (int i = 10; i <= 43; i++) {
                            if (i % 9 == 0 || i % 9 == 8) continue;
                            ItemStack sellItem2 = sellInv.getItem(i);
                            if (sellItem2 == null) continue;
                            if (sellItem2.hasItemMeta() && sellItem2.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING)) {
                                continue;
                            }
                            if (sellItem2.getType().name().endsWith("SHULKER_BOX")) {
                                // Nur leere Shulkers entfernen (die mit Inhalt bleiben)
                                if (sellItem2.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bm
                                        && bm.getBlockState() instanceof org.bukkit.block.ShulkerBox sb) {
                                    boolean hasItems = false;
                                    for (ItemStack si : sb.getInventory().getContents()) {
                                        if (si != null && !si.getType().isAir()) { hasItems = true; break; }
                                    }
                                    if (!hasItems && plugin.getWorthManager().getWorth(sellItem2) > 0) {
                                        sellInv.setItem(i, null);
                                    }
                                }
                                continue;
                            }
                            double worth = plugin.getWorthManager().getWorth(sellItem2);
                            if (worth > 0) {
                                sellInv.setItem(i, null);
                            }
                        }

                        // Sell Multiplier
                        double bonus = plugin.getSellMultiplier().trackSaleBatch(p15, itemsToSell,
                                item -> plugin.getWorthManager().getWorth(item));
                        double finalTotal = total + bonus;

                        // Geld geben
                        plugin.getEconomy().deposit(p15.getUniqueId(), finalTotal);
                        p15.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ §e" + itemsSold + " Items §afür §e" + NumberFormatter.formatMoney(finalTotal) + " §averkauft!"
                                + (bonus > 0 ? " §8(§7+" + NumberFormatter.formatMoney(bonus) + " Bonus§8)" : ""));
                        p15.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§7Neuer Kontostand: §a" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(p15.getUniqueId())));
                        p15.playSound(p15.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);

                        // Update Worth Display
                        de.coolemod.donut.gui.SellGUI.updateWorthDisplay(plugin, sellInv);
                        return;
                    } else if (action.equals("sell_close")) {
                        org.bukkit.entity.Player p16 = (org.bukkit.entity.Player)e.getWhoClicked();
                        org.bukkit.inventory.Inventory sellInv = e.getView().getTopInventory();

                        // Gib alle Items zurück
                        for (int i = 10; i <= 43; i++) {
                            if (i % 9 == 0 || i % 9 == 8) continue;
                            ItemStack returnItem = sellInv.getItem(i);
                            if (returnItem == null || returnItem.getType() == Material.AIR) continue;
                            if (returnItem.hasItemMeta() && returnItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING)) {
                                continue;
                            }
                            // Blockiere ALLE Glass Panes
                            if (returnItem.getType().name().contains("GLASS_PANE")) continue;
                            // Blockiere GUI-Elemente
                            if (returnItem.getType() == Material.WRITABLE_BOOK || returnItem.getType() == Material.ARROW ||
                                returnItem.getType() == Material.GOLD_INGOT || returnItem.getType() == Material.BARRIER ||
                                returnItem.getType() == Material.EMERALD || returnItem.getType() == Material.SUNFLOWER) continue;

                            // Gib Item zurück
                            java.util.HashMap<Integer, ItemStack> leftover = p16.getInventory().addItem(returnItem);
                            if (!leftover.isEmpty()) {
                                for (ItemStack left : leftover.values()) {
                                    p16.getWorld().dropItemNaturally(p16.getLocation(), left);
                                }
                            }
                        }

                        p16.closeInventory();
                        return;
                    } else if (action.equals("sell_worth_display")) {
                        // Nur Display, nichts tun
                        return;
                    }
            }
        }

        // Slay Shop Klicks - erweiterte Kauflogik
        if (title.contains("Slay Shop")) {
            e.setCancelled(true);
            // Nur LEFT-Click erlauben
            if (e.getClick() != org.bukkit.event.inventory.ClickType.LEFT) {
                return;
            }
            // Blockiere UI-Elemente (Panes, Navigation Buttons, etc.)
            if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE ||
                clicked.getType() == Material.GRAY_STAINED_GLASS_PANE ||
                clicked.getType() == Material.ARROW ||
                clicked.getType() == Material.EMERALD ||
                clicked.getType() == Material.BARRIER) {
                return;
            }
            if (!clicked.hasItemMeta()) return;
            org.bukkit.inventory.meta.ItemMeta meta = clicked.getItemMeta();
            org.bukkit.entity.Player buyer = (org.bukkit.entity.Player) e.getWhoClicked();
            // Prüfe shop_cost in PDC
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_cost"), PersistentDataType.INTEGER)) {
                int cost = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "shop_cost"), PersistentDataType.INTEGER);
                if (plugin.getShards().removeShards(buyer.getUniqueId(), cost)) {
                    ItemStack give = clicked.clone();
                    give.setItemMeta(null); // entferne shop meta
                    give = new ItemStack(clicked.getType(), clicked.getAmount() > 0 ? clicked.getAmount() : 1);
                    buyer.getInventory().addItem(give);
                    buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Gekauft für §d" + cost + " Shards§a!");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.not-enough-shards", ""));
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
            return;
        }

        // Schul Shop Klicks - erweiterte Kauflogik mit Geld
        if (title.contains("SCHUL") || title.contains("SHOP") || title.contains("FOOD") || title.contains("GEAR") || title.contains("NETHER") || title.contains("SHARDS")) {
            // KRITISCH: Cancelle ALLES - keine Ausnahmen!
            e.setCancelled(true);
            e.setResult(org.bukkit.event.Event.Result.DENY);

            // Blockiere Klicks im eigenen Inventar KOMPLETT
            if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) {
                e.setCancelled(true);
                e.setResult(org.bukkit.event.Event.Result.DENY);
                return;
            }

            // Blockiere ALLE Click-Typen außer LEFT-Click
            if (e.getClick() != org.bukkit.event.inventory.ClickType.LEFT) {
                e.setCancelled(true);
                e.setResult(org.bukkit.event.Event.Result.DENY);
                return;
            }

            // Blockiere NUMBER_KEY, DROP, HOTBAR Klicks explizit
            if (e.getClick().name().contains("NUMBER") || e.getClick().name().contains("DROP") ||
                e.getClick().name().contains("HOTBAR") || e.getClick().name().contains("SWAP")) {
                e.setCancelled(true);
                e.setResult(org.bukkit.event.Event.Result.DENY);
                return;
            }

            // Wenn kein Item geklickt wurde
            if (clicked == null || !clicked.hasItemMeta()) {
                return;
            }

            org.bukkit.inventory.meta.ItemMeta meta = clicked.getItemMeta();
            org.bukkit.entity.Player buyer = (org.bukkit.entity.Player) e.getWhoClicked();

            // Shop-Kategorie-Navigation (öffnet Unterkategorie)
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_category"), PersistentDataType.STRING)) {
                String category = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "shop_category"), PersistentDataType.STRING);
                ShopGUI shopGUI = new ShopGUI(plugin);
                buyer.playSound(buyer.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);

                if (category.contains("FOOD")) {
                    shopGUI.openFoodShop(buyer);
                } else if (category.contains("GEAR")) {
                    shopGUI.openGearShop(buyer);
                } else if (category.contains("NETHER")) {
                    shopGUI.openNetherShop(buyer);
                } else if (category.contains("END")) {
                    buyer.sendMessage("§8┃ §5§lEND SHOP §8┃ §cNoch nicht verfügbar!");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                } else if (category.contains("SHARD")) {
                    shopGUI.openShardShop(buyer);
                }
                return;
            }

            // Zurück-Button (geht zurück zum Hauptmenü)
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_back"), PersistentDataType.STRING)) {
                buyer.playSound(buyer.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                new ShopGUI(plugin).open(buyer);
                return;
            }

            // Schließen-Button
            if (clicked.getType() == Material.BARRIER) {
                if (clicked.hasItemMeta() && clicked.getItemMeta().getDisplayName().contains("SCHLIESSEN")) {
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                    buyer.closeInventory();
                    return;
                }
            }

            // Balance-Info Item (keine Aktion)
            if (clicked.getType() == Material.GOLD_INGOT) {
                return; // Nur Info, keine Aktion
            }

            // Info-Item (keine Aktion)
            if (clicked.getType() == Material.PAPER) {
                return; // Nur Info, keine Aktion
            }

            // Kauflogik mit Geld (kauft Item und gibt es dem Spieler)
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_cost_money"), PersistentDataType.INTEGER)) {
                int cost = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "shop_cost_money"), PersistentDataType.INTEGER);
                int amount = meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_amount"), PersistentDataType.INTEGER)
                    ? meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "shop_amount"), PersistentDataType.INTEGER)
                    : 1;

                double balance = plugin.getEconomy().getBalance(buyer.getUniqueId());

                if (balance >= cost && plugin.getEconomy().withdraw(buyer.getUniqueId(), cost)) {
                    // Erstelle NEUES Item ohne PDC (nur Material + Amount)
                    ItemStack give = new ItemStack(clicked.getType(), amount);

                    // Gib Item ins Inventar
                    java.util.HashMap<Integer, ItemStack> leftover = buyer.getInventory().addItem(give);

                    // Falls Inventar voll, droppe Items
                    if (!leftover.isEmpty()) {
                        for (ItemStack left : leftover.values()) {
                            buyer.getWorld().dropItemNaturally(buyer.getLocation(), left);
                        }
                        buyer.sendMessage("§8┃ §e⚠ Inventar voll! Items gedroppt.");
                    }

                    buyer.sendMessage("");
                    buyer.sendMessage("§8┃ §6§lSCHUL SHOP §8┃ §a§l✓ GEKAUFT!");
                    buyer.sendMessage("§8┃ §7Item§8: §f" + clicked.getType().name() + " §8x" + amount);
                    buyer.sendMessage("§8┃ §7Preis§8: §e$" + cost);
                    buyer.sendMessage("§8┃ §7Neuer Kontostand§8: §a" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(buyer.getUniqueId())));
                    buyer.sendMessage("");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    buyer.sendMessage("");
                    buyer.sendMessage("§8┃ §c§l✖ SCHUL SHOP §8┃ §cNicht genug Geld!");
                    buyer.sendMessage("§8┃ §7Benötigt§8: §e$" + cost);
                    buyer.sendMessage("§8┃ §7Dein Geld§8: §e" + NumberFormatter.formatMoney(balance));
                    buyer.sendMessage("§8┃ §7Fehlt§8: §c" + NumberFormatter.formatMoney(cost - balance));
                    buyer.sendMessage("");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                return; // Event bleibt cancelled - Item bleibt im Shop!
            }

            // Kauflogik mit Shards (für Spawner)
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_cost_shards"), PersistentDataType.INTEGER)) {
                int cost = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "shop_cost_shards"), PersistentDataType.INTEGER);
                int amount = meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_amount"), PersistentDataType.INTEGER)
                    ? meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "shop_amount"), PersistentDataType.INTEGER)
                    : 1;
                String spawnerType = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "spawner_type"), PersistentDataType.STRING);

                if (plugin.getShards().removeShards(buyer.getUniqueId(), cost)) {
                    // Gib Spawner mit richtigem Typ (neues SpawnerType System)
                    if (spawnerType != null && clicked.getType() == Material.SPAWNER) {
                        de.coolemod.donut.systems.SpawnerType type = de.coolemod.donut.systems.SpawnerType.fromName(spawnerType);
                        if (type != null) {
                            ItemStack spawner = plugin.getSpawnerManager().createSpawnerItem(type);
                            buyer.getInventory().addItem(spawner);
                        } else {
                            // Fallback: EntityType
                            try {
                                org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(spawnerType);
                                ItemStack spawner = plugin.getSpawnerManager().createSpawnerItem(entityType);
                                if (spawner != null) buyer.getInventory().addItem(spawner);
                                else {
                                    ItemStack give = new ItemStack(clicked.getType(), amount);
                                    buyer.getInventory().addItem(give);
                                }
                            } catch (Exception ex) {
                                ItemStack give = new ItemStack(clicked.getType(), amount);
                                buyer.getInventory().addItem(give);
                            }
                        }
                    } else {
                        ItemStack give = new ItemStack(clicked.getType(), amount);
                        buyer.getInventory().addItem(give);
                    }
                    buyer.sendMessage("");
                    buyer.sendMessage("§8┃ §d§lSHARD SHOP §8┃ §a§l✓ GEKAUFT!");
                    buyer.sendMessage("§8┃ §7Item§8: §f" + clicked.getItemMeta().getDisplayName());
                    buyer.sendMessage("§8┃ §7Preis§8: §d" + cost + " Shards");
                    buyer.sendMessage("");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                } else {
                    buyer.sendMessage("");
                    buyer.sendMessage("§8┃ §c§l✖ SHARD SHOP §8┃ §cNicht genug Shards!");
                    buyer.sendMessage("§8┃ §7Benötigt§8: §d" + cost + " Shards");
                    buyer.sendMessage("§8┃ §7Deine Shards§8: §d" + plugin.getShards().getShards(buyer.getUniqueId()));
                    buyer.sendMessage("");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                return; // WICHTIG: Verhindert, dass Item herausgenommen wird
            }
            return;
        }

        // Auktionshaus: Kauf-Logik bei Klick auf Artikel
        if (title.contains("Auktionshaus") || title.contains("ᴀᴜᴋᴛɪᴏɴѕʜᴀᴜѕ") || title.contains("AUKTIONSHAUS") || title.contains("Meine Auktionen") || title.contains("ᴍᴇɪɴᴇ ᴀᴜᴋᴛɪᴏɴᴇɴ") || title.contains("MEINE AUKTIONEN")) {
            e.setCancelled(true);

            // Blockiere ALLE Click-Typen außer LEFT_CLICK und RIGHT_CLICK
            if (e.getClick() != org.bukkit.event.inventory.ClickType.LEFT &&
                e.getClick() != org.bukkit.event.inventory.ClickType.RIGHT) {
                return;
            }

            if (!clicked.hasItemMeta()) return;
            org.bukkit.inventory.meta.ItemMeta meta = clicked.getItemMeta();
            org.bukkit.entity.Player buyer = (org.bukkit.entity.Player) e.getWhoClicked();

            // Blockiere ALLE Navigation/UI Items komplett - kein weiteres Processing
            if (clicked.getType() == Material.ARROW || clicked.getType() == Material.EMERALD ||
                clicked.getType() == Material.BARRIER || clicked.getType() == Material.ENDER_CHEST ||
                clicked.getType() == Material.GOLD_BLOCK || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE ||
                clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) {
                return; // Komplett blockieren
            }

            // Kauf (Linksklick auf Auktions-Items)
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "auction_id"), PersistentDataType.STRING) && e.getClick().isLeftClick()) {
                String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "auction_id"), PersistentDataType.STRING);
                if (plugin.getAuctionManager().buy(buyer, id)) {
                    buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Kauf erfolgreich!");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    buyer.closeInventory();
                } else {
                    buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Kauf fehlgeschlagen.");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                return;
            }

            // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
            // Stornieren (Rechtsklick in "Meine Auktionen")
            // if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "auction_cancel"), PersistentDataType.STRING) && e.getClick().isRightClick()) {
            //     String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "auction_cancel"), PersistentDataType.STRING);
            //     if (plugin.getAuctionManager().cancel(buyer.getUniqueId(), id)) {
            //         buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Auktion zurückgezogen!");
            //         buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            //         new AuctionGUI(plugin).openMyAuctions(buyer);
            //     } else {
            //         buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Konnte Auktion nicht zurückziehen.");
            //     }
            //     return;
            // }
            return;
        }

        // Orders: Lieferung per Klick
        if (title.contains("Orders") || title.contains("Meine Orders")) {
            e.setCancelled(true);
            // Nur LEFT und RIGHT Click erlauben
            if (e.getClick() != org.bukkit.event.inventory.ClickType.LEFT &&
                e.getClick() != org.bukkit.event.inventory.ClickType.RIGHT) {
                return;
            }
            // Blockiere UI-Elemente (Panes, Navigation Buttons, etc.)
            if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE ||
                clicked.getType() == Material.GRAY_STAINED_GLASS_PANE ||
                clicked.getType() == Material.ARROW ||
                clicked.getType() == Material.EMERALD ||
                clicked.getType() == Material.BARRIER ||
                clicked.getType() == Material.PAPER) {
                return;
            }
            if (e.isShiftClick()) return;
            if (!clicked.hasItemMeta()) return;
            org.bukkit.inventory.meta.ItemMeta meta = clicked.getItemMeta();
            org.bukkit.entity.Player player = (org.bukkit.entity.Player) e.getWhoClicked();

            // Liefern (Linksklick in "Orders")
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "order_id"), PersistentDataType.STRING) && e.getClick().isLeftClick()) {
                String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "order_id"), PersistentDataType.STRING);
                OrdersManager.Order order = null;
                for (OrdersManager.Order o : plugin.getOrdersManager().getAll()) if (o.id.equals(id)) { order = o; break; }
                if (order == null) { player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Order nicht gefunden."); return; }
                int need = order.requiredAmount - order.delivered;
                if (need <= 0) { player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Order bereits abgeschlossen."); return; }
                int have = 0;
                org.bukkit.inventory.PlayerInventory inv = player.getInventory();
                for (ItemStack is2 : inv.getContents()) {
                    if (is2 == null) continue;
                    if (is2.getType() == order.itemType.getType()) have += is2.getAmount();
                }
                if (have <= 0) { player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Du hast keine passenden Items."); return; }
                int deliverAmount = Math.min(have, need);
                int toRemove = deliverAmount;
                for (int idx = 0; idx < inv.getSize() && toRemove > 0; idx++) {
                    ItemStack is2 = inv.getItem(idx);
                    if (is2 == null) continue;
                    if (is2.getType() == order.itemType.getType()) {
                        int use = Math.min(is2.getAmount(), toRemove);
                        is2.setAmount(is2.getAmount() - use);
                        if (is2.getAmount() <= 0) inv.setItem(idx, null);
                        else inv.setItem(idx, is2);
                        toRemove -= use;
                    }
                }
                plugin.getOrdersManager().deliverToOrder(id, player.getUniqueId(), deliverAmount);
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ " + deliverAmount + " Items geliefert! Verdient: §a" + NumberFormatter.formatMoney(deliverAmount * order.pricePerItem));
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                player.closeInventory();
                return;
            }

            // Stornieren (Rechtsklick in "Meine Orders")
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "order_cancel"), PersistentDataType.STRING) && e.getClick().isRightClick()) {
                String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "order_cancel"), PersistentDataType.STRING);
                if (plugin.getOrdersManager().cancelOrder(player.getUniqueId(), id)) {
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Order storniert und Geld erstattet!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                    player.closeInventory();
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.openInventory(plugin.getOrderSystem().createMyOrdersGUI(player.getUniqueId()));
                    }, 2L);
                } else {
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Konnte Order nicht stornieren.");
                }
                return;
            }
            return;
        }

        // Kisten Animation: Blockiere ALLE Klicks während der Öffnung
        if (title.contains("§6Öffne:") || title.contains("Öffne:")) {
            e.setCancelled(true);
            return;
        }

        // Kisten: öffne Detailansicht bei Klick
        if (title.contains("Kiste") || title.contains("KISTE")) {
            e.setCancelled(true);
            if (e.isShiftClick()) return;
            if (!clicked.hasItemMeta()) return;
            org.bukkit.inventory.meta.ItemMeta meta = clicked.getItemMeta();

            // Nur Items mit donut_gui_action oder donut_crate_id erlauben
            if (!meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING) &&
                !meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_crate_id"), PersistentDataType.STRING)) {
                return; // Alle anderen Items (Pool, Guaranteed, etc.) blockieren
            }

            // Kiste öffnen via donut_crate_id
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_crate_id"), PersistentDataType.STRING)) {
                String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "donut_crate_id"), PersistentDataType.STRING);
                org.bukkit.entity.Player player = (org.bukkit.entity.Player) e.getWhoClicked();
                if (plugin.getCrateManager().hasCrate(id)) {
                    new de.coolemod.donut.gui.CrateDetailGUI(plugin).open(player, id);
                }
                return;
            }
            // Navigation Actions werden durch donut_gui_action weiter unten behandelt
            return;
        }
    }
}
