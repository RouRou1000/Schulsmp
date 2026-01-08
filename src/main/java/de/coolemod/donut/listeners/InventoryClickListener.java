package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.AuctionGUI;
import de.coolemod.donut.gui.OrdersGUI;
import de.coolemod.donut.gui.SlayShopGUI;
import de.coolemod.donut.gui.ShopGUI;
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

        // Allgemein: alle Menü-Klicks abbrechen (außer Sell-GUI und AuctionCreate-GUI)
        if (title.contains("Slay Shop") || title.contains("Shop") || title.contains("ᴀᴜᴋᴛɪᴏɴѕʜᴀᴜѕ") || title.contains("AUKTIONSHAUS") || title.contains("Orders") || title.contains("Kiste") || title.contains("DONUT CORE") || title.contains("ᴍᴇɪɴᴇ ᴀᴜᴋᴛɪᴏɴᴇɴ") || title.contains("MEINE AUKTIONEN")) {
            e.setCancelled(true);
        }
        
        // Sell-GUI: Erlaube Item-Platzierung, aber blockiere Buttons
        if (title.contains("ɪᴛᴇᴍѕ ᴠᴇʀᴋᴀᴜꜰᴇɴ") || title.contains("ITEMS VERKAUFEN")) {
            ItemStack current = e.getCurrentItem();
            int slot = e.getRawSlot();
            
            // Wenn im Top-Inventory (GUI)
            if (slot < 54) {
                // Blockiere ALLE Items die GUI-Komponenten sind
                if (current != null && current.hasItemMeta() && current.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING)) {
                    e.setCancelled(true);
                    return;
                } else if (current != null && (current.getType() == Material.BLACK_STAINED_GLASS_PANE || current.getType() == Material.GRAY_STAINED_GLASS_PANE)) {
                    e.setCancelled(true);
                    return;
                } else if (current != null && (current.getType() == Material.WRITABLE_BOOK || current.getType() == Material.EMERALD || current.getType() == Material.BARRIER || current.getType() == Material.ARROW)) {
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
                // Jetzt blockiere alle anderen UI-Elemente
                else if (current != null && (current.getType() == Material.BLACK_STAINED_GLASS_PANE || current.getType() == Material.GRAY_STAINED_GLASS_PANE)) {
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
        }

        // Prüfe AH-Aktionen
        NamespacedKey ahKey = new NamespacedKey(plugin, "ah_action");
        if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(ahKey, PersistentDataType.STRING)) {
            String ahAction = clicked.getItemMeta().getPersistentDataContainer().get(ahKey, PersistentDataType.STRING);
            org.bukkit.Bukkit.getLogger().info("AH Action: " + ahAction);
            switch (ahAction) {
                case "setprice":
                    org.bukkit.entity.Player p_price = (org.bukkit.entity.Player)e.getWhoClicked();
                    org.bukkit.Bukkit.getLogger().info("Opening price input for " + p_price.getName());
                    
                    // Speichere Items aus der GUI
                    de.coolemod.donut.gui.AuctionCreateGUI.saveItems(p_price, e.getView().getTopInventory());
                    
                    // Markiere Spieler als in Preis-Eingabe (damit Items nicht zurückgegeben werden)
                    p_price.setMetadata("ah_setting_price", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
                    
                    // Schließe GUI erst, dann öffne Sign im nächsten Tick
                    p_price.closeInventory();
                    org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            new de.coolemod.donut.gui.AnvilInputGUI(plugin).openPriceInput(p_price);
                        } catch (Exception ex) {
                            org.bukkit.Bukkit.getLogger().severe("Error in setprice: " + ex.getMessage());
                            ex.printStackTrace();
                            p_price.sendMessage("§cFehler beim Öffnen der Preis-Eingabe!");
                            p_price.removeMetadata("ah_setting_price", plugin);
                        }
                    }, 2L);
                    return;
                case "confirm":
                    org.bukkit.entity.Player p_confirm = (org.bukkit.entity.Player)e.getWhoClicked();
                    org.bukkit.inventory.Inventory createInv = e.getView().getTopInventory();
                    
                    // Prüfe Preis aus HashMap
                    double price = de.coolemod.donut.gui.AuctionCreateGUI.getPrice(p_confirm);
                    if (price <= 0) {
                        p_confirm.sendMessage("§cKein Preis gesetzt!");
                        return;
                    }
                    
                    // Sammle Items
                    java.util.List<ItemStack> auctionItems = new java.util.ArrayList<>();
                    for (int i = 10; i <= 43; i++) {
                        if (i % 9 == 0 || i % 9 == 8) continue;
                        ItemStack aItem = createInv.getItem(i);
                        if (aItem == null || aItem.getType() == Material.AIR) continue;
                        if (aItem.hasItemMeta() && aItem.getItemMeta().getPersistentDataContainer().has(ahKey, PersistentDataType.STRING)) continue;
                        if (aItem.getType() == Material.BLACK_STAINED_GLASS_PANE || aItem.getType() == Material.GRAY_STAINED_GLASS_PANE) continue;
                        auctionItems.add(aItem.clone());
                    }
                    
                    if (auctionItems.isEmpty()) {
                        p_confirm.sendMessage("§cKeine Items in der Auktion!");
                        return;
                    }
                    
                    // Erstelle Auktionen
                    for (ItemStack auctionItem : auctionItems) {
                        plugin.getAuctionManager().listItem(p_confirm.getUniqueId(), auctionItem, price);
                    }
                    
                    // Lösche Items aus GUI
                    for (int i = 10; i <= 43; i++) {
                        if (i % 9 == 0 || i % 9 == 8) continue;
                        ItemStack delItem = createInv.getItem(i);
                        if (delItem == null || delItem.getType() == Material.AIR) continue;
                        if (delItem.hasItemMeta() && delItem.getItemMeta().getPersistentDataContainer().has(ahKey, PersistentDataType.STRING)) continue;
                        if (delItem.getType() == Material.BLACK_STAINED_GLASS_PANE || delItem.getType() == Material.GRAY_STAINED_GLASS_PANE) continue;
                        createInv.setItem(i, null);
                    }
                    
                    p_confirm.closeInventory();
                    de.coolemod.donut.gui.AuctionCreateGUI.clearPrice(p_confirm);
                    p_confirm.sendMessage("§a✓ " + auctionItems.size() + " Auktion(en) erstellt für je $" + "%.2f".formatted(price) + "!");
                    p_confirm.playSound(p_confirm.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    return;
                case "back":
                    org.bukkit.entity.Player p_back = (org.bukkit.entity.Player)e.getWhoClicked();
                    p_back.closeInventory();
                    new AuctionGUI(plugin).open(p_back, 1);
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
                case "auction_prev":
                case "auction_next":
                    new AuctionGUI(plugin).open((org.bukkit.entity.Player)e.getWhoClicked(), page);
                    return;
                case "auction_close":
                    ((org.bukkit.entity.Player)e.getWhoClicked()).closeInventory();
                    return;
                case "auction_my":
                    new AuctionGUI(plugin).openMyAuctions((org.bukkit.entity.Player)e.getWhoClicked());
                    return;
                case "auction_back":
                    new AuctionGUI(plugin).open((org.bukkit.entity.Player)e.getWhoClicked(), 1);
                    return;
                case "auction_browse":
                    new AuctionGUI(plugin).open((org.bukkit.entity.Player)e.getWhoClicked(), 1);
                    return;
                case "auction_create_close":
                    org.bukkit.entity.Player p_close = (org.bukkit.entity.Player)e.getWhoClicked();
                    org.bukkit.inventory.Inventory closeInv = e.getView().getTopInventory();
                    
                    // Gib alle Items zurück
                    for (int i = 10; i <= 43; i++) {
                        if (i % 9 == 0 || i % 9 == 8) continue;
                        ItemStack returnItem = closeInv.getItem(i);
                        if (returnItem == null || returnItem.getType() == Material.AIR) continue;
                        if (returnItem.hasItemMeta() && returnItem.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING)) {
                            continue;
                        }
                        if (returnItem.getType() == Material.BLACK_STAINED_GLASS_PANE || returnItem.getType() == Material.GRAY_STAINED_GLASS_PANE) continue;
                        
                        // Gib Item zurück
                        java.util.HashMap<Integer, ItemStack> leftover = p_close.getInventory().addItem(returnItem);
                        if (!leftover.isEmpty()) {
                            for (ItemStack left : leftover.values()) {
                                p_close.getWorld().dropItemNaturally(p_close.getLocation(), left);
                            }
                        }
                    }
                    
                    p_close.closeInventory();
                    return;
                case "auction_create":
                    // Öffne Create-GUI für neue Auktion
                    org.bukkit.entity.Player pl_create = (org.bukkit.entity.Player)e.getWhoClicked();
                    new de.coolemod.donut.gui.AuctionCreateGUI(plugin).open(pl_create);
                    return;
                case "orders_prev":
                case "orders_next":
                    new OrdersGUI(plugin).open((org.bukkit.entity.Player)e.getWhoClicked(), page);
                    return;
                case "orders_close":
                    ((org.bukkit.entity.Player)e.getWhoClicked()).closeInventory();
                    return;
                case "orders_my":
                    new OrdersGUI(plugin).openMyOrders((org.bukkit.entity.Player)e.getWhoClicked());
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
                        // openCrateAnimated konsumiert bereits den Schlüssel
                        plugin.getCrateManager().openCrateAnimated(p3, id);
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
                        if (!p5.hasPermission("donut.admin")) { p5.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Keine Berechtigung."); return; }
                        // Admin test: skip key check
                        plugin.getCrateManager().openCrateAnimated(p5, id, true);
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
                        org.bukkit.entity.Player p9 = (org.bukkit.entity.Player)e.getWhoClicked();
                        new de.coolemod.donut.gui.AuctionCreateGUI(plugin).open(p9);
                        return;
                    } else if (action.equals("open_orders")) {
                        org.bukkit.entity.Player p10 = (org.bukkit.entity.Player)e.getWhoClicked();
                        new de.coolemod.donut.gui.OrdersGUI(plugin).open(p10);
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
                        p13.closeInventory();
                        p13.performCommand("worth");
                        return;
                    } else if (action.equals("close_menu")) {
                        org.bukkit.entity.Player p14 = (org.bukkit.entity.Player)e.getWhoClicked();
                        p14.closeInventory();
                        return;
                    } else if (action.equals("sell_items")) {
                        org.bukkit.entity.Player p15 = (org.bukkit.entity.Player)e.getWhoClicked();
                        org.bukkit.inventory.Inventory sellInv = e.getView().getTopInventory();
                        
                        // Sammle alle Items aus den Sell-Slots
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
                        
                        // Entferne Items
                        for (int i = 10; i <= 43; i++) {
                            if (i % 9 == 0 || i % 9 == 8) continue;
                            ItemStack sellItem2 = sellInv.getItem(i);
                            if (sellItem2 == null) continue;
                            if (sellItem2.hasItemMeta() && sellItem2.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING)) {
                                continue;
                            }
                            double worth = plugin.getWorthManager().getWorth(sellItem2);
                            if (worth > 0) {
                                sellInv.setItem(i, null);
                            }
                        }
                        
                        // Geld geben
                        plugin.getEconomy().deposit(p15.getUniqueId(), total);
                        p15.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ §e" + itemsSold + " Items §afür §e$" + "%.2f".formatted(total) + " §averkauft!");
                        p15.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§7Neuer Kontostand: §a$" + "%.2f".formatted(plugin.getEconomy().getBalance(p15.getUniqueId())));
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
                            if (returnItem.getType() == Material.BLACK_STAINED_GLASS_PANE || returnItem.getType() == Material.GRAY_STAINED_GLASS_PANE) continue;
                            
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

        // Donut Shop Klicks - erweiterte Kauflogik mit Geld
        if (title.contains("Donut Shop")) {
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
            if (e.isShiftClick()) return;
            if (!clicked.hasItemMeta()) return;
            org.bukkit.inventory.meta.ItemMeta meta = clicked.getItemMeta();
            org.bukkit.entity.Player buyer = (org.bukkit.entity.Player) e.getWhoClicked();
            // Navigation actions
            String action = meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING) 
                ? meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING) : null;
            if ("shop_close".equals(action)) { buyer.closeInventory(); return; }
            if ("open_auction".equals(action)) { new de.coolemod.donut.gui.AuctionCreateGUI(plugin).open(buyer); return; }
            // Kauflogik
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_cost_money"), PersistentDataType.INTEGER)) {
                int cost = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "shop_cost_money"), PersistentDataType.INTEGER);
                if (plugin.getEconomy().withdraw(buyer.getUniqueId(), cost)) {
                    ItemStack give = new ItemStack(clicked.getType(), clicked.getAmount() > 0 ? clicked.getAmount() : 1);
                    buyer.getInventory().addItem(give);
                    buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Gekauft für §a$" + cost + "§a!");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                } else {
                    buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + plugin.getConfig().getString("messages.not-enough-money", ""));
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
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
            
            // Stornieren (Rechtsklick in "Meine Auktionen")
            if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "auction_cancel"), PersistentDataType.STRING) && e.getClick().isRightClick()) {
                String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "auction_cancel"), PersistentDataType.STRING);
                if (plugin.getAuctionManager().cancel(buyer.getUniqueId(), id)) {
                    buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Auktion zurückgezogen!");
                    buyer.playSound(buyer.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
                    new AuctionGUI(plugin).openMyAuctions(buyer);
                } else {
                    buyer.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Konnte Auktion nicht zurückziehen.");
                }
                return;
            }
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
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ " + deliverAmount + " Items geliefert! Verdient: §a$" + "%.2f".formatted(deliverAmount * order.pricePerItem));
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
                    new OrdersGUI(plugin).openMyOrders(player);
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
        if (title.contains("Kiste")) {
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
