package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.guis.AuctionCreateGUINew;
import de.coolemod.donut.guis.AuctionGUINew;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Dedicated auction event handler
 * - Clean separation from other GUI handlers
 * - Strict UI element checking
 * - Anti-dupe protection
 */
public class AuctionEventHandler implements Listener {
    private final DonutPlugin plugin;
    private final AuctionGUINew auctionGUI;
    private final AuctionCreateGUINew createGUI;
    
    private final NamespacedKey UI_KEY;
    
    public AuctionEventHandler(DonutPlugin plugin, AuctionGUINew auctionGUI, AuctionCreateGUINew createGUI) {
        this.plugin = plugin;
        this.auctionGUI = auctionGUI;
        this.createGUI = createGUI;
        this.UI_KEY = new NamespacedKey(plugin, "ui_element");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) e.getWhoClicked();
        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null) return;
        
        String title = e.getView().getTitle();
        
        // Check if it's an auction GUI
        boolean isAuction = auctionGUI.isAuctionGUI(title);
        boolean isCreate = createGUI.isCreateGUI(title);
        
        if (!isAuction && !isCreate) return;
        
        ItemStack clicked = e.getCurrentItem();
        ItemStack cursor = e.getCursor();
        int slot = e.getRawSlot();
        
        // Allow player inventory clicks in create GUI
        if (isCreate && slot >= 45) {
            // Player clicked their own inventory - allow it
            e.setCancelled(false);
            return;
        }
        
        // ALWAYS cancel first for safety
        e.setCancelled(true);
        
        // Special handling for Create GUI slot 22 (item placement)
        if (isCreate && slot == 22) {
            // Get session to check if price is already set
            AuctionCreateGUINew.CreateSession session = createGUI.getSession(player.getUniqueId());
            
            if (session != null && session.price > 0) {
                // Price is set, block all interactions with slot 22
                player.sendMessage("§cDas Item kann nicht mehr entfernt werden! Nutze den Zurück-Button.");
                return;
            }
            
            // Allow item placement/removal in slot 22 (only if price not set)
            e.setCancelled(false);
            
            // Update session after item placed/removed
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                ItemStack newItem = clickedInv.getItem(22);
                if (newItem != null && !newItem.getType().isAir()) {
                    // Item was placed
                    createGUI.handleClick(player, newItem, slot);
                }
            }, 2L);
            return;
        }
        
        // Handle UI element clicks (buttons)
        if (isUIElement(clicked)) {
            e.setResult(org.bukkit.event.Event.Result.DENY);
            e.setCursor(null);
            
            // Handle action
            if (isAuction) {
                auctionGUI.handleClick(player, clicked, slot, clickedInv);
            } else if (isCreate) {
                createGUI.handleClick(player, clicked, slot);
            }
            return;
        }
        
        // Block cursor items with UI marker
        if (isUIElement(cursor)) {
            e.setResult(org.bukkit.event.Event.Result.DENY);
            e.setCursor(null);
            player.updateInventory();
            return;
        }
        
        // Block all number key presses (shift-click etc)
        if (e.getClick().name().contains("NUMBER")) {
            e.setResult(org.bukkit.event.Event.Result.DENY);
            return;
        }
        
        // Additional safety: block any glass pane or common UI materials
        if (clicked != null) {
            String typeName = clicked.getType().name();
            if (typeName.contains("GLASS_PANE") || 
                typeName.equals("BARRIER") ||
                typeName.equals("ARROW")) {
                
                e.setResult(org.bukkit.event.Event.Result.DENY);
                e.setCursor(null);
                player.updateInventory();
                
                // But still handle click for navigation
                if (isAuction) {
                    auctionGUI.handleClick(player, clicked, slot, clickedInv);
                } else if (isCreate) {
                    createGUI.handleClick(player, clicked, slot);
                }
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        
        String title = e.getView().getTitle();
        
        if (auctionGUI.isAuctionGUI(title) || createGUI.isCreateGUI(title)) {
            // Block ALL drag operations in auction GUIs
            e.setCancelled(true);
            e.setResult(org.bukkit.event.Event.Result.DENY);
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        
        Player player = (Player) e.getPlayer();
        String title = e.getView().getTitle();
        
        // Clean up create session if GUI closed
        if (createGUI.isCreateGUI(title)) {
            createGUI.closeSession(player);
        }
    }
    
    /**
     * Check if item is marked as UI element
     */
    private boolean isUIElement(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(UI_KEY, PersistentDataType.STRING);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(SignChangeEvent e) {
        Player player = e.getPlayer();
        
        // Check if player has active auction creation session
        if (createGUI == null) return;
        
        AuctionCreateGUINew.CreateSession session = createGUI.getSession(player.getUniqueId());
        if (session == null) return;
        
        // Parse price from first line
        String input = e.getLine(0);
        if (input == null || input.trim().isEmpty()) {
            player.sendMessage("§cKeine Eingabe!");
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(session.gui);
            });
            return;
        }
        
        // Remove sign
        Block block = e.getBlock();
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            block.setType(Material.AIR);
        });
        
        try {
            double price = Double.parseDouble(input.trim());
            
            if (price <= 0) {
                player.sendMessage("§cDer Preis muss größer als 0 sein!");
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    player.openInventory(session.gui);
                });
                return;
            }
            
            // Set price and reopen GUI
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                createGUI.setPrice(player, price);
            });
            
        } catch (NumberFormatException ex) {
            player.sendMessage("§cUngültige Eingabe! Bitte gib eine Zahl ein.");
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                player.openInventory(session.gui);
            });
        }
    }
}
