package de.coolemod.donut.guis;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.AuctionHouseManagerNew;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Neu implementierte Auktions-Erstellungs-GUI
 * - Alle UI-Elemente haben PDC-Keys VOR dem GUI-Öffnen
 * - Session-basiert ohne static state
 * - Thread-safe
 */
public class AuctionCreateGUINew {
    private final DonutPlugin plugin;
    private final AuctionHouseManagerNew manager;
    
    // Session tracking: Player UUID -> CreateSession
    private final Map<UUID, CreateSession> sessions = new ConcurrentHashMap<>();
    
    // PDC Keys
    private final NamespacedKey UI_KEY;
    private final NamespacedKey ACTION_KEY;
    
    public AuctionCreateGUINew(DonutPlugin plugin, AuctionHouseManagerNew manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.UI_KEY = new NamespacedKey(plugin, "ui_element");
        this.ACTION_KEY = new NamespacedKey(plugin, "action");
    }
    
    /**
     * Auction creation states
     */
    public enum CreationState {
        PLACING_ITEM,    // User can place item in slot 22
        PRICE_SET,       // Price set, item locked, can confirm or go back
        COMPLETED        // Auction created successfully
    }
    
    public static class CreateSession {
        public ItemStack item;
        public double price;
        public Inventory gui;
        public CreationState state;
        
        CreateSession() {
            this.price = 0;
            this.state = CreationState.PLACING_ITEM;
        }
    }
    
    /**
     * Convert to small caps
     */
    private String toSmallCaps(String text) {
        return text.replace("A", "ᴀ").replace("B", "ʙ").replace("C", "ᴄ")
                .replace("D", "ᴅ").replace("E", "ᴇ").replace("F", "ғ")
                .replace("G", "ɢ").replace("H", "ʜ").replace("I", "ɪ")
                .replace("J", "ᴊ").replace("K", "ᴋ").replace("L", "ʟ")
                .replace("M", "ᴍ").replace("N", "ɴ").replace("O", "ᴏ")
                .replace("P", "ᴘ").replace("Q", "ǫ").replace("R", "ʀ")
                .replace("S", "s").replace("T", "ᴛ").replace("U", "ᴜ")
                .replace("V", "ᴠ").replace("W", "ᴡ").replace("X", "x")
                .replace("Y", "ʏ").replace("Z", "ᴢ");
    }
    
    /**
     * Mark item as UI element - GUARANTEED to be set before GUI opens
     */
    private ItemStack markAsUIElement(ItemStack item, String action) {
        if (item == null) return null;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(UI_KEY, PersistentDataType.STRING, "true");
        pdc.set(ACTION_KEY, PersistentDataType.STRING, action);
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Create border item with PDC
     */
    private ItemStack createBorder() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName("§0");
        border.setItemMeta(meta);
        return markAsUIElement(border, "border");
    }
    
    /**     * Create placeholder for item slot
     */
    private ItemStack createPlaceholder() {
        ItemStack placeholder = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = placeholder.getItemMeta();
        meta.setDisplayName("§7§lItem hier platzieren");
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7Ziehe ein Item aus deinem");
        lore.add("§7Inventar in diesen Slot");
        lore.add("");
        lore.add("§e▸ Klicke mit einem Item");
        meta.setLore(lore);
        placeholder.setItemMeta(meta);
        return markAsUIElement(placeholder, "placeholder");
    }
    
    /**     * Create price button
     */
    private ItemStack createPriceButton(double currentPrice) {
        ItemStack button = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName("§6§lPreis festlegen");
        
        List<String> lore = new ArrayList<>();
        if (currentPrice > 0) {
            lore.add("§7Aktuell: §e$" + String.format("%.2f", currentPrice));
        } else {
            lore.add("§7Kein Preis gesetzt!");
        }
        lore.add("");
        lore.add("§eKlicken zum Ändern");
        meta.setLore(lore);
        
        button.setItemMeta(meta);
        return markAsUIElement(button, "setprice");
    }
    
    /**
     * Create confirm button
     */
    private ItemStack createConfirmButton(boolean hasPrice) {
        ItemStack button;
        ItemMeta meta;
        
        if (hasPrice) {
            button = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            meta = button.getItemMeta();
            meta.setDisplayName("§a§l✓ Auktion erstellen");
            List<String> lore = new ArrayList<>();
            lore.add("§7Klicke um zu bestätigen");
            meta.setLore(lore);
        } else {
            button = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            meta = button.getItemMeta();
            meta.setDisplayName("§c§l✗ Preis erforderlich");
            List<String> lore = new ArrayList<>();
            lore.add("§7Bitte zuerst Preis festlegen!");
            meta.setLore(lore);
        }
        
        button.setItemMeta(meta);
        return markAsUIElement(button, hasPrice ? "confirm" : "disabled");
    }
    
    /**
     * Create back button
     */
    private ItemStack createBackButton() {
        ItemStack button = new ItemStack(Material.ARROW);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName("§c« Zurück");
        button.setItemMeta(meta);
        return markAsUIElement(button, "back");
    }
    
    /**
     * Create disabled price button
     */
    private ItemStack createPriceButtonDisabled() {
        ItemStack button = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName("§7§lPreis festlegen");
        List<String> lore = new ArrayList<>();
        lore.add("§7Zuerst Item platzieren!");
        meta.setLore(lore);
        button.setItemMeta(meta);
        return markAsUIElement(button, "setprice_disabled");
    }
    
    /**
     * Open GUI to create auction - player can place any item
     */
    public void openEmpty(Player player) {
        // Create session
        CreateSession session = new CreateSession();
        session.item = null;  // No item yet
        session.price = 0;
        
        // Create GUI - 45 slots (5 rows)
        String title = "§8" + toSmallCaps("AUKTION ERSTELLEN");
        session.gui = Bukkit.createInventory(null, 45, title);
        
        // Fill all slots with borders first
        ItemStack border = createBorder();
        for (int i = 0; i < 45; i++) {
            session.gui.setItem(i, border);
        }
        
        // Clear middle slot (slot 22 - center of 5 rows) for item placement
        // Leave slot 22 empty for item placement (set to null/air)
        session.gui.setItem(22, null);
        
        // Back button (slot 20)
        session.gui.setItem(20, createBackButton());
        
        // Price button (slot 24) - disabled initially
        session.gui.setItem(24, createPriceButtonDisabled());
        
        // Store session
        sessions.put(player.getUniqueId(), session);
        
        // Open GUI
        player.openInventory(session.gui);
    }
    
    /**
     * Open GUI with item to auction
     */
    public void open(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            // If no item, open empty GUI
            openEmpty(player);
            return;
        }
        
        // Create session
        CreateSession session = new CreateSession();
        session.item = item.clone();
        session.price = 0;
        
        // Create GUI - 45 slots (5 rows)
        String title = "§8" + toSmallCaps("AUKTION ERSTELLEN");
        session.gui = Bukkit.createInventory(null, 45, title);
        
        // Fill all slots with borders
        ItemStack border = createBorder();
        for (int i = 0; i < 45; i++) {
            session.gui.setItem(i, border);
        }
        
        // Item display in middle (slot 22)
        ItemStack display = session.item.clone();
        ItemMeta displayMeta = display.getItemMeta();
        if (displayMeta != null) {
            List<String> lore = displayMeta.hasLore() ? new ArrayList<>(displayMeta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§7Dieses Item wird verkauft");
            displayMeta.setLore(lore);
            display.setItemMeta(displayMeta);
        }
        session.gui.setItem(22, display);
        
        // Back button (slot 20)
        session.gui.setItem(20, createBackButton());
        
        // Price button (slot 24)
        session.gui.setItem(24, createPriceButton(0));
        
        // Store session
        sessions.put(player.getUniqueId(), session);
        
        // Open GUI
        player.openInventory(session.gui);
    }
    
    /**
     * Public method to update GUI (called from event handler)
     */
    public void updateGUIPublic(CreateSession session) {
        updateGUI(session);
    }
    
    /**
     * Update GUI based on session state
     */
    private void updateGUI(CreateSession session) {
        if (session == null || session.gui == null) return;
        
        boolean hasItem = session.item != null && !session.item.getType().isAir();
        boolean hasPrice = session.price > 0;
        
        // Update state
        if (hasItem && hasPrice) {
            session.state = CreationState.PRICE_SET;
        } else if (hasItem) {
            session.state = CreationState.PLACING_ITEM;
        }
        
        // Clear slots
        session.gui.setItem(22, null);
        session.gui.setItem(24, null);
        session.gui.setItem(40, null);
        
        // Update item display (slot 22)
        if (hasItem) {
            ItemStack display = session.item.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                if (session.state == CreationState.PRICE_SET) {
                    lore.add("§a§l✓ Gesperrt");
                    lore.add("§7Preis: §e$" + String.format("%.2f", session.price));
                    lore.add("");
                    lore.add("§cKann nicht mehr entfernt werden");
                } else {
                    lore.add("§7Dieses Item wird verkauft");
                    lore.add("");
                    lore.add("§eSetze einen Preis →");
                }
                meta.setLore(lore);
                display.setItemMeta(meta);
            }
            // Mark as UI element if price is set (prevents taking)
            if (session.state == CreationState.PRICE_SET) {
                display = markAsUIElement(display, "locked_item");
            }
            session.gui.setItem(22, display);
        } else {
            session.gui.setItem(22, createPlaceholder());
        }
        
        // Update price button (slot 24)
        if (hasItem) {
            session.gui.setItem(24, hasPrice ? createPriceButton(session.price) : createPriceButton(0));
        } else {
            session.gui.setItem(24, createPriceButtonDisabled());
        }
        
        // Update confirm button (slot 40)
        if (session.state == CreationState.PRICE_SET) {
            session.gui.setItem(40, createConfirmButton(true));
        } else {
            session.gui.setItem(40, createBorder());
        }
    }
    
    /**
     * Handle click in GUI
     */
    public boolean handleClick(Player player, ItemStack clicked, int slot) {
        CreateSession session = sessions.get(player.getUniqueId());
        if (session == null) return false;
        
        // Special handling for slot 22 (item placement)
        if (slot == 22) {
            // Check if it's the placeholder
            if (clicked != null && clicked.getType() == Material.PAPER) {
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.getDisplayName().contains("Item hier platzieren")) {
                    // It's the placeholder - will be replaced by player's item
                    return false; // Allow the action
                }
            }
            // If player placed an item, update session
            if (clicked != null && !clicked.getType().isAir()) {
                session.item = clicked.clone();
                // Enable price button
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    CreateSession updatedSession = sessions.get(player.getUniqueId());
                    if (updatedSession != null) {
                        updateGUI(updatedSession);
                    }
                }, 2L);
            }
            return false; // Allow item interaction
        }
        
        // Check if clicked item is UI element
        if (clicked != null && clicked.hasItemMeta()) {
            ItemMeta meta = clicked.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            
            if (!pdc.has(UI_KEY, PersistentDataType.STRING)) {
                // Not a UI element
                return false;
            }
            
            String action = pdc.get(ACTION_KEY, PersistentDataType.STRING);
            if (action == null) return true;
            
            switch (action) {
                case "setprice":
                    if (session.item == null || session.item.getType().isAir()) {
                        player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                            "§cBitte zuerst ein Item platzieren!");
                        return true;
                    }
                    // Open Sign GUI for price input
                    openPriceInput(player);
                    return true;
                    
                case "setprice_disabled":
                    player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                        "§cBitte zuerst ein Item in Slot 22 platzieren!");
                    return true;
                    
                case "confirm":
                    if (session.price <= 0) {
                        player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                            "§cBitte zuerst Preis festlegen!");
                        return true;
                    }
                    
                    // Create auction
                    String auctionId = manager.listItem(player.getUniqueId(), session.item, session.price);
                    if (auctionId != null) {
                        player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                            "§a✓ Auktion erstellt für §e$" + String.format("%.2f", session.price));
                        // Clear item from session to prevent duplication
                        session.item = null;
                        player.closeInventory();
                        sessions.remove(player.getUniqueId());
                    } else {
                        player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                            "§cFehler beim Erstellen der Auktion!");
                    }
                    return true;
                    
                case "back":
                    // Return item to player before closing
                    if (session.item != null && !session.item.getType().isAir()) {
                        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(session.item.clone());
                        if (!leftover.isEmpty()) {
                            for (ItemStack drop : leftover.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), drop);
                            }
                        }
                    }
                    player.closeInventory();
                    sessions.remove(player.getUniqueId());
                    return true;
                    
                case "border":
                case "disabled":
                    return true;
            }
        }
        
        return true;
    }
    
    /**
     * Set price for session
     */
    public void setPrice(Player player, double price) {
        CreateSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage("§cKeine aktive Auktions-Erstellung!");
            return;
        }
        
        if (price <= 0) {
            player.sendMessage("§cPreis muss größer als 0 sein!");
            player.openInventory(session.gui);
            return;
        }
        
        // Set price and lock item
        session.price = price;
        session.state = CreationState.PRICE_SET;
        updateGUI(session);
        
        // Reopen GUI
        player.openInventory(session.gui);
        player.sendMessage("§a✓ Preis festgelegt: §e$" + String.format("%.2f", price));
        player.sendMessage("§7Item ist jetzt gesperrt. Klicke §a✓ Fertig §7zum Bestätigen.");
    }
    
    /**
     * Open Sign GUI for price input
     */
    public void openPriceInput(Player player) {
        CreateSession session = sessions.get(player.getUniqueId());
        if (session == null) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                "§cKeine aktive Auktions-Erstellung!");
            return;
        }
        
        try {
            // Create temporary sign above player
            org.bukkit.block.Block block = player.getLocation().add(0, 3, 0).getBlock();
            Material originalType = block.getType();
            
            // Set sign
            block.setType(Material.OAK_SIGN);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setLine(0, "");
            sign.setLine(1, "^^^^^^^^^^^^^^");
            sign.setLine(2, "Preis eingeben");
            sign.setLine(3, "");
            sign.update(false, false);
            
            // Open sign for editing
            player.openSign(sign);
            
            // Remove sign after 10 seconds if not edited
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                block.setType(originalType);
            }, 200L);
            
        } catch (Exception ex) {
            player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                "§cFehler beim Öffnen der Preis-Eingabe!");
            player.openInventory(session.gui);
        }
    }
    
    /**
     * Cleanup session when GUI is closed
     */
    public void closeSession(Player player) {
        CreateSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        
        // Only return item if state is PLACING_ITEM (price not set yet)
        if (session.state == CreationState.PLACING_ITEM && session.item != null && !session.item.getType().isAir()) {
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(session.item.clone());
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            player.sendMessage("§7Item zurückgegeben.");
        }
        // If state is PRICE_SET, item is lost unless back button was used
    }
    
    /**
     * Check if inventory is this GUI
     */
    public boolean isCreateGUI(String title) {
        return title != null && title.contains(toSmallCaps("AUKTION ERSTELLEN"));
    }
    
    /**
     * Check if player has active session
     */
    public boolean hasSession(UUID playerId) {
        return sessions.containsKey(playerId);
    }
    
    /**
     * Get session for UUID (used by event handler)
     */
    public CreateSession getSession(UUID uuid) {
        return sessions.get(uuid);
    }
}
