package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.guis.AuctionCreateGUI_V2;
import de.coolemod.donut.guis.AuctionGUINew;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Komplett neuer Event Handler - EINFACH UND KLAR
 */
public class AuctionHandler_V2 implements Listener {
    private final DonutPlugin plugin;
    private final AuctionGUINew auctionGUI;
    private final AuctionCreateGUI_V2 createGUI;
    
    public AuctionHandler_V2(DonutPlugin plugin, AuctionGUINew auctionGUI, AuctionCreateGUI_V2 createGUI) {
        this.plugin = plugin;
        this.auctionGUI = auctionGUI;
        this.createGUI = createGUI;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        
        String title = e.getView().getTitle();
        boolean isCreate = createGUI.isCreateGUI(title);
        boolean isAuction = auctionGUI.isAuctionGUI(title);
        
        if (!isCreate && !isAuction) return;
        
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor = e.getCursor();
        
        // ===== AUCTION BROWSE GUI =====
        if (isAuction) {
            e.setCancelled(true);
            if (slot < 54 && auctionGUI.isUIElement(clicked)) {
                auctionGUI.handleClick(player, clicked, slot, e.getClickedInventory());
            }
            return;
        }
        
        // ===== CREATE GUI =====
        if (isCreate) {
            AuctionCreateGUI_V2.Session session = createGUI.getSession(player.getUniqueId());
            if (session == null) {
                e.setCancelled(true);
                return;
            }
            
            // Erlaube Klicks im Player-Inventar
            if (slot >= 45) {
                e.setCancelled(false);
                return;
            }
            
            // Cancele alle Top-Inventory-Klicks
            e.setCancelled(true);
            
            player.sendMessage("§7[DEBUG] Slot: " + slot + " | Material: " + (clicked != null ? clicked.getType() : "NULL"));
            
            // Prüfe ob geklicktes Item ein UI-Element ist (Button oder Placeholder)
            if (clicked != null && createGUI.isUIElement(clicked)) {
                String action = createGUI.getAction(clicked);
                player.sendMessage("§7[DEBUG] UI-Element erkannt! Action: " + action);
                
                // Slot 22: Item Placement/Removal (Placeholder oder Item)
                if (slot == 22) {
                    handleSlot22(e, player, session, clicked, cursor);
                    return;
                }
                
                // Button Clicks (andere Slots)
                if (action != null && !action.equals("border")) {
                    createGUI.handleButtonClick(player, action);
                }
                return;
            } else {
                player.sendMessage("§c[DEBUG] KEIN UI-Element! isNull=" + (clicked == null) + " | hasItemMeta=" + (clicked != null && clicked.hasItemMeta()));
            }
            
            // Slot 22: Real item (not UI element) - player wants to take it
            if (slot == 22 && clicked != null && !clicked.getType().isAir()) {
                handleSlot22(e, player, session, clicked, cursor);
                return;
            }
            
            // Prevent placing items in wrong slots - return item to cursor
            if (cursor != null && !cursor.getType().isAir()) {
                // Item wird in falschen Slot gelegt, blockiere es aber lösche es nicht
                // Event ist bereits gecancelt, nichts weiter zu tun
            }
        }
    }
    
    /**
     * Handle Slot 22 Interaction (Item Slot)
     */
    private void handleSlot22(InventoryClickEvent e, Player player, AuctionCreateGUI_V2.Session session, ItemStack clicked, ItemStack cursor) {
        // Fall 1: Es ist der Placeholder -> Spieler legt Item rein
        String action = createGUI.getAction(clicked);
        if ("placeholder".equals(action)) {
            if (cursor != null && !cursor.getType().isAir()) {
                // Spieler legt Item auf Placeholder
                if (session.priceLocked) {
                    player.sendMessage("§cDu kannst das Item nicht ändern nach Preis-Eingabe!");
                    return;
                }
                
                ItemStack toPlace = cursor.clone();
                toPlace.setAmount(1);
                
                // Entferne 1 vom Cursor
                if (cursor.getAmount() > 1) {
                    cursor.setAmount(cursor.getAmount() - 1);
                } else {
                    e.setCursor(null);
                }
                
                // Speichere in Session (refreshGUI wird intern aufgerufen)
                createGUI.handleItemPlacement(player, toPlace);
            }
            return;
        }
        
        // Fall 2: Item ist im Slot -> Spieler will es nehmen
        if (clicked != null && !clicked.getType().isAir() && !createGUI.isUIElement(clicked)) {
            if (session.priceLocked) {
                player.sendMessage("§cItem ist gesperrt! Benutze §eZurück §cum es zurückzuerhalten.");
                return;
            }
            
            // Erlaube Item-Entnahme nur mit Linksklick
            if (e.getClick() == ClickType.LEFT) {
                // Gebe das gespeicherte Item zurück
                if (session.storedItem != null) {
                    player.getInventory().addItem(session.storedItem);
                    createGUI.handleItemRemoval(player);
                }
            }
        }
    }
    
    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (createGUI.isCreateGUI(title) || auctionGUI.isAuctionGUI(title)) {
            e.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        
        String title = e.getView().getTitle();
        if (createGUI.isCreateGUI(title)) {
            createGUI.cleanup(player);
        }
    }
    
    @EventHandler
    public void onSign(SignChangeEvent e) {
        Player player = e.getPlayer();
        AuctionCreateGUI_V2.Session session = createGUI.getSession(player.getUniqueId());
        
        if (session == null) return;
        
        String input = e.getLine(0);
        if (input == null || input.trim().isEmpty()) {
            player.sendMessage("§cKeine Eingabe!");
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(session.gui);
            });
            return;
        }
        
        // Entferne Sign
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            e.getBlock().setType(Material.AIR);
        });
        
        try {
            double price = Double.parseDouble(input.trim());
            
            if (price <= 0) {
                player.sendMessage("§cPreis muss größer als 0 sein!");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    player.openInventory(session.gui);
                });
                return;
            }
            
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                createGUI.setPrice(player, price);
            });
            
        } catch (NumberFormatException ex) {
            player.sendMessage("§cUngültige Zahl!");
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(session.gui);
            });
        }
    }
}
