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

/**
 * Neu implementierte Auktionshaus-Browse-GUI
 * - Alle UI-Elemente mit PDC markiert
 * - Pagination
 * - Sichere Item-Display
 */
public class AuctionGUINew {
    private final DonutPlugin plugin;
    private final AuctionHouseManagerNew manager;
    
    private final NamespacedKey UI_KEY;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey AUCTION_ID_KEY;
    
    private static final int ITEMS_PER_PAGE = 28; // 7x4 grid
    
    public AuctionGUINew(DonutPlugin plugin, AuctionHouseManagerNew manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.UI_KEY = new NamespacedKey(plugin, "ui_element");
        this.ACTION_KEY = new NamespacedKey(plugin, "action");
        this.AUCTION_ID_KEY = new NamespacedKey(plugin, "auction_id");
    }
    
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
    
    private ItemStack createBorder() {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName("§8");
        border.setItemMeta(meta);
        return markAsUIElement(border, "border");
    }
    
    private ItemStack createAuctionDisplay(AuctionHouseManagerNew.Auction auction) {
        ItemStack display = auction.item.clone();
        ItemMeta meta = display.getItemMeta();
        
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add("§7Preis: §e$" + String.format("%.2f", auction.price));
        lore.add("§7Verkäufer: §f" + plugin.getServer().getOfflinePlayer(auction.seller).getName());
        lore.add("");
        lore.add("§a§lKlicken zum Kaufen");
        
        meta.setLore(lore);
        display.setItemMeta(meta);
        
        // Mark as auction item with ID
        ItemMeta finalMeta = display.getItemMeta();
        PersistentDataContainer pdc = finalMeta.getPersistentDataContainer();
        pdc.set(UI_KEY, PersistentDataType.STRING, "true");
        pdc.set(ACTION_KEY, PersistentDataType.STRING, "buy");
        pdc.set(AUCTION_ID_KEY, PersistentDataType.STRING, auction.id);
        display.setItemMeta(finalMeta);
        
        return display;
    }
    
    private ItemStack createPageButton(String direction, boolean enabled) {
        ItemStack button;
        if (enabled) {
            button = new ItemStack(Material.ARROW);
        } else {
            button = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        }
        
        ItemMeta meta = button.getItemMeta();
        if (direction.equals("next")) {
            meta.setDisplayName(enabled ? "§a→ Nächste Seite" : "§7Letzte Seite");
        } else {
            meta.setDisplayName(enabled ? "§a← Vorherige Seite" : "§7Erste Seite");
        }
        button.setItemMeta(meta);
        return markAsUIElement(button, enabled ? direction : "disabled");
    }
    
    private ItemStack createInfoButton() {
        ItemStack button = new ItemStack(Material.BOOK);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName("§e§lAuktionshaus");
        List<String> lore = new ArrayList<>();
        lore.add("§7Hier kannst du Items");
        lore.add("§7kaufen und verkaufen!");
        meta.setLore(lore);
        button.setItemMeta(meta);
        return markAsUIElement(button, "info");
    }
    
    private ItemStack createMyAuctionsButton() {
        ItemStack button = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName("§d§lMeine Auktionen");
        List<String> lore = new ArrayList<>();
        lore.add("§7Zeige deine aktiven Auktionen");
        meta.setLore(lore);
        button.setItemMeta(meta);
        return markAsUIElement(button, "myauctions");
    }
    
    private ItemStack createNewAuctionButton() {
        ItemStack button = new ItemStack(Material.PAPER);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName("§a§l+ Neue Auktion");
        List<String> lore = new ArrayList<>();
        lore.add("§7Klicke um eine neue");
        lore.add("§7Auktion zu erstellen!");
        lore.add("");
        lore.add("§eLege Items ein und");
        lore.add("§esetze einen Preis");
        meta.setLore(lore);
        button.setItemMeta(meta);
        return markAsUIElement(button, "newauction");
    }
    
    /**
     * Open browse GUI
     */
    public void open(Player player, int page) {
        String title = "§8" + toSmallCaps("AUKTIONSHAUS");
        Inventory gui = Bukkit.createInventory(null, 54, title + " §8(" + (page + 1) + ")");
        
        // Borders
        ItemStack border = createBorder();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(i + 45, border);
        }
        for (int row = 1; row < 5; row++) {
            gui.setItem(row * 9, border);
            gui.setItem(row * 9 + 8, border);
        }
        
        // Get auctions
        List<AuctionHouseManagerNew.Auction> auctions = new ArrayList<>(manager.getAll());
        int totalPages = (int) Math.ceil((double) auctions.size() / ITEMS_PER_PAGE);
        if (page >= totalPages) page = Math.max(0, totalPages - 1);
        
        // Display auction items (slots 10-43, excluding borders)
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, auctions.size());
        
        int[] slots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        
        for (int i = startIndex; i < endIndex; i++) {
            AuctionHouseManagerNew.Auction auction = auctions.get(i);
            gui.setItem(slots[i - startIndex], createAuctionDisplay(auction));
        }
        
        // Navigation
        gui.setItem(48, createPageButton("prev", page > 0));
        gui.setItem(49, createInfoButton());
        gui.setItem(50, createMyAuctionsButton());
        gui.setItem(51, createPageButton("next", page < totalPages - 1));
        gui.setItem(53, createNewAuctionButton());
        
        player.openInventory(gui);
    }
    
    /**
     * Open player's own auctions
     */
    public void openMyAuctions(Player player) {
        String title = "§8" + toSmallCaps("MEINE AUKTIONEN");
        Inventory gui = Bukkit.createInventory(null, 54, title);
        
        // Borders
        ItemStack border = createBorder();
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, border);
            gui.setItem(i + 45, border);
        }
        for (int row = 1; row < 5; row++) {
            gui.setItem(row * 9, border);
            gui.setItem(row * 9 + 8, border);
        }
        
        // Get player's auctions
        List<AuctionHouseManagerNew.Auction> auctions = manager.getByOwner(player.getUniqueId());
        
        // Add "Create New Auction" button in top left (slot 10)
        gui.setItem(10, createNewAuctionButton());
        
        int[] slots = {
            11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        
        for (int i = 0; i < Math.min(auctions.size(), slots.length); i++) {
            AuctionHouseManagerNew.Auction auction = auctions.get(i);
            ItemStack display = auction.item.clone();
            ItemMeta meta = display.getItemMeta();
            
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§7Preis: §e$" + String.format("%.2f", auction.price));
            lore.add("");
            lore.add("§c§lKlicken zum Zurückziehen");
            
            meta.setLore(lore);
            display.setItemMeta(meta);
            
            // Mark as cancel action
            ItemMeta finalMeta = display.getItemMeta();
            PersistentDataContainer pdc = finalMeta.getPersistentDataContainer();
            pdc.set(UI_KEY, PersistentDataType.STRING, "true");
            pdc.set(ACTION_KEY, PersistentDataType.STRING, "cancel");
            pdc.set(AUCTION_ID_KEY, PersistentDataType.STRING, auction.id);
            display.setItemMeta(finalMeta);
            
            gui.setItem(slots[i], display);
        }
        
        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§c« Zurück");
        back.setItemMeta(backMeta);
        gui.setItem(49, markAsUIElement(back, "back"));
        
        player.openInventory(gui);
    }
    
    /**
     * Handle click
     */
    public boolean handleClick(Player player, ItemStack clicked, int slot, Inventory inv) {
        if (clicked == null || !clicked.hasItemMeta()) return false;
        
        ItemMeta meta = clicked.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (!pdc.has(UI_KEY, PersistentDataType.STRING)) {
            return false;
        }
        
        String action = pdc.get(ACTION_KEY, PersistentDataType.STRING);
        if (action == null) return true;
        
        switch (action) {
            case "buy":
                String buyId = pdc.get(AUCTION_ID_KEY, PersistentDataType.STRING);
                if (buyId != null) {
                    if (manager.buy(player, buyId)) {
                        player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                            "§a✓ Item gekauft!");
                        player.closeInventory();
                        // Reopen to refresh
                        new org.bukkit.scheduler.BukkitRunnable() {
                            @Override
                            public void run() {
                                open(player, 0);
                            }
                        }.runTaskLater(plugin, 2L);
                    } else {
                        player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                            "§cKauf fehlgeschlagen! Nicht genug Geld?");
                    }
                }
                return true;
                
            case "cancel":
                String cancelId = pdc.get(AUCTION_ID_KEY, PersistentDataType.STRING);
                if (cancelId != null) {
                    if (manager.cancel(player.getUniqueId(), cancelId)) {
                        player.closeInventory();
                        new org.bukkit.scheduler.BukkitRunnable() {
                            @Override
                            public void run() {
                                openMyAuctions(player);
                            }
                        }.runTaskLater(plugin, 2L);
                    }
                }
                return true;
                
            case "myauctions":
                openMyAuctions(player);
                return true;
                
            case "newauction":
                // Open creation GUI - DISABLED (using new system)
                player.closeInventory();
                player.sendMessage("§cUse /ah for the new auction system!");
                return true;
                
            case "back":
                open(player, 0);
                return true;
                
            case "prev":
                // Extract page from title
                String title = inv.getViewers().get(0) != null ? 
                    inv.getViewers().get(0).getOpenInventory().getTitle() : "";
                int currentPage = extractPage(title);
                open(player, currentPage - 1);
                return true;
                
            case "next":
                title = inv.getViewers().get(0) != null ? 
                    inv.getViewers().get(0).getOpenInventory().getTitle() : "";
                currentPage = extractPage(title);
                open(player, currentPage + 1);
                return true;
                
            case "border":
            case "info":
            case "disabled":
                return true;
        }
        
        return true;
    }
    
    private int extractPage(String title) {
        try {
            if (title.contains("(") && title.contains(")")) {
                String pageStr = title.substring(title.indexOf("(") + 1, title.indexOf(")"));
                return Integer.parseInt(pageStr) - 1;
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0;
    }
    
    public boolean isAuctionGUI(String title) {
        return title != null && (title.contains(toSmallCaps("AUKTIONSHAUS")) || 
                                  title.contains(toSmallCaps("MEINE AUKTIONEN")));
    }
    
    public boolean isUIElement(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(UI_KEY, PersistentDataType.STRING);
    }
}
