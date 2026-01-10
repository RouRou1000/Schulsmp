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
 * Completely rewritten auction event handler
 * - Simple and robust event handling
 * - State-based item locking
 * - Anti-dupe protection
 */
public class AuctionEventHandlerNew implements Listener {
    private final DonutPlugin plugin;
    private final AuctionGUINew auctionGUI;
    private final AuctionCreateGUINew createGUI;
    private final NamespacedKey UI_KEY;
    
    public AuctionEventHandlerNew(DonutPlugin plugin, AuctionGUINew auctionGUI, AuctionCreateGUINew createGUI) {
        this.plugin = plugin;
        this.auctionGUI = auctionGUI;
        this.createGUI = createGUI;
        this.UI_KEY = new NamespacedKey(plugin, "ui_element");
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) e.getWhoClicked();
        Inventory clickedInv = e.getClickedInventory();
        if (clickedInv == null) return;
        
        String title = e.getView().getTitle();
        boolean isAuction = auctionGUI.isAuctionGUI(title);
        boolean isCreate = createGUI.isCreateGUI(title);
        
        if (!isAuction && !isCreate) return;
        
        ItemStack clicked = e.getCurrentItem();
        int slot = e.getRawSlot();
        
        // Allow player inventory clicks in create GUI
        if (isCreate && slot >= 45) {
            e.setCancelled(false);
            return;
        }
        
        // Default: cancel all clicks
        e.setCancelled(true);
        
        // Handle auction GUI
        if (isAuction) {
            if (isUIElement(clicked)) {
                auctionGUI.handleClick(player, clicked, slot, clickedInv);
            }
            return;
        }
        
        // Handle create GUI
        if (isCreate) {
            AuctionCreateGUINew.CreateSession session = createGUI.getSession(player.getUniqueId());
            if (session == null) return;
            
            // Slot 22: Item placement/removal
            if (slot == 22) {
                handleItemSlotClick(e, player, session, clicked);
                return;
            }
            
            // Handle button clicks
            if (isUIElement(clicked)) {
                createGUI.handleClick(player, clicked, slot);
            }
        }
    }
    
    /**
     * Handle click on slot 22 (item slot)
     */
    private void handleItemSlotClick(InventoryClickEvent e, Player player, AuctionCreateGUINew.CreateSession session, ItemStack clicked) {
        // Check if price is set - if so, item is locked
        if (session.state == AuctionCreateGUINew.CreationState.PRICE_SET) {
            player.sendMessage("§cItem ist gesperrt! Benutze §eZurück §cum es zurückzuerhalten.");
            return;
        }
        
        // Check if it's the placeholder
        if (clicked != null && clicked.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                // Player is placing an item on placeholder
                ItemStack placedItem = cursor.clone();
                placedItem.setAmount(1);
                session.item = placedItem;
                
                // Remove 1 from cursor
                if (cursor.getAmount() > 1) {
                    cursor.setAmount(cursor.getAmount() - 1);
                } else {
                    e.setCursor(null);
                }
                
                // Update GUI
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    createGUI.updateGUIPublic(session);
                });
            }
            return;
        }
        
        // Player wants to take item back (only if price not set)
        if (clicked != null && !clicked.getType().isAir() && !isUIElement(clicked)) {
            // Allow taking item back
            session.item = null;
            e.setCancelled(false);
            
            // Update GUI
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                createGUI.updateGUIPublic(session);
            });
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        
        String title = e.getView().getTitle();
        boolean isAuction = auctionGUI.isAuctionGUI(title);
        boolean isCreate = createGUI.isCreateGUI(title);
        
        if (isAuction || isCreate) {
            // Block all dragging in auction GUIs
            e.setCancelled(true);
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
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onSignChange(SignChangeEvent e) {
        Player player = e.getPlayer();
        
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
}
