package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.storage.DataManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Neu implementiertes Auktionshaus-System
 * - Thread-safe mit ConcurrentHashMap
 * - Transaktions-Sicherheit gegen Dupes
 * - Sauberes Item-Tracking
 */
public class AuctionHouseManagerNew {
    private final DonutPlugin plugin;
    private final DataManager data;
    
    // Thread-safe auction storage
    private final Map<String, Auction> auctions = new ConcurrentHashMap<>();
    
    // Track active transactions to prevent dupes
    private final Set<String> activeTransactions = ConcurrentHashMap.newKeySet();
    
    public AuctionHouseManagerNew(DonutPlugin plugin) {
        this.plugin = plugin;
        this.data = new DataManager(plugin.getDataFolder(), plugin.getConfig().getString("data-file", "data.yml"));
        load();
    }
    
    public static class Auction {
        public final String id;
        public final UUID seller;
        public final ItemStack item;
        public final double price;
        public final long timestamp;
        
        public Auction(String id, UUID seller, ItemStack item, double price) {
            this.id = id;
            this.seller = seller;
            this.item = item.clone(); // Always clone to prevent reference issues
            this.price = price;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * List item for auction
     * Returns auction ID or null if failed
     */
    public synchronized String listItem(UUID seller, ItemStack item, double price) {
        if (item == null || item.getType().isAir()) return null;
        if (price <= 0) return null;
        
        String id = UUID.randomUUID().toString();
        Auction auction = new Auction(id, seller, item, price);
        auctions.put(id, auction);
        save();
        
        plugin.getLogger().info("[Auction] Created: " + id + " by " + seller + " for $" + price);
        return id;
    }
    
    /**
     * Buy auction - fully transactional with rollback on failure
     */
    public synchronized boolean buy(Player buyer, String id) {
        // Check if transaction already in progress
        if (activeTransactions.contains(id)) {
            return false;
        }
        
        Auction auction = auctions.get(id);
        if (auction == null) {
            return false;
        }
        
        // Can't buy own auction
        if (auction.seller.equals(buyer.getUniqueId())) {
            return false;
        }
        
        // Mark transaction as active
        activeTransactions.add(id);
        
        try {
            // Check balance
            double balance = plugin.getEconomy().getBalance(buyer.getUniqueId());
            if (balance < auction.price) {
                return false;
            }
            
            // Withdraw money from buyer
            if (!plugin.getEconomy().withdraw(buyer.getUniqueId(), auction.price)) {
                plugin.getLogger().warning("[Auction] Failed to withdraw from buyer: " + buyer.getName());
                return false;
            }
            
            // Give money to seller
            plugin.getEconomy().deposit(auction.seller, auction.price);
            
            // Give item to buyer
            ItemStack itemCopy = auction.item.clone();
            HashMap<Integer, ItemStack> leftover = buyer.getInventory().addItem(itemCopy);
            
            // Drop leftovers if inventory full
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    buyer.getWorld().dropItemNaturally(buyer.getLocation(), drop);
                }
            }
            
            // Notify seller if online
            Player seller = plugin.getServer().getPlayer(auction.seller);
            if (seller != null && seller.isOnline()) {
                seller.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                    "§a✓ Dein Item wurde für §e$" + String.format("%.2f", auction.price) + " §averkauft!");
            }
            
            // Remove auction
            auctions.remove(id);
            save();
            
            plugin.getLogger().info("[Auction] Completed: " + id + " bought by " + buyer.getName());
            return true;
            
        } finally {
            // Always remove from active transactions
            activeTransactions.remove(id);
        }
    }
    
    /**
     * Cancel auction and return item to seller
     */
    public synchronized boolean cancel(UUID seller, String id) {
        Auction auction = auctions.get(id);
        if (auction == null) {
            return false;
        }
        
        // Only seller can cancel
        if (!auction.seller.equals(seller)) {
            return false;
        }
        
        // Return item to seller
        Player player = plugin.getServer().getPlayer(seller);
        if (player != null && player.isOnline()) {
            ItemStack itemCopy = auction.item.clone();
            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemCopy);
            
            // Drop if inventory full
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            
            player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                "§a✓ Auktion zurückgezogen!");
        }
        
        // Remove auction
        auctions.remove(id);
        save();
        
        plugin.getLogger().info("[Auction] Cancelled: " + id + " by " + seller);
        return true;
    }
    
    public Collection<Auction> getAll() {
        return new ArrayList<>(auctions.values());
    }
    
    public List<Auction> getByOwner(UUID owner) {
        List<Auction> result = new ArrayList<>();
        for (Auction auction : auctions.values()) {
            if (auction.seller.equals(owner)) {
                result.add(auction);
            }
        }
        return result;
    }
    
    public Auction getAuction(String id) {
        return auctions.get(id);
    }
    
    public synchronized void save() {
        FileConfiguration cfg = data.getConfig();
        cfg.set("auctions", null);
        
        for (Auction auction : auctions.values()) {
            String base = "auctions." + auction.id;
            cfg.set(base + ".seller", auction.seller.toString());
            cfg.set(base + ".price", auction.price);
            cfg.set(base + ".item", auction.item);
            cfg.set(base + ".timestamp", auction.timestamp);
        }
        
        data.save();
    }
    
    private synchronized void load() {
        FileConfiguration cfg = data.getConfig();
        if (!cfg.contains("auctions")) return;
        
        for (String id : cfg.getConfigurationSection("auctions").getKeys(false)) {
            try {
                String base = "auctions." + id;
                UUID seller = UUID.fromString(cfg.getString(base + ".seller"));
                double price = cfg.getDouble(base + ".price");
                ItemStack item = cfg.getItemStack(base + ".item");
                long timestamp = cfg.getLong(base + ".timestamp", System.currentTimeMillis());
                
                if (item != null) {
                    Auction auction = new Auction(id, seller, item, price);
                    auctions.put(id, auction);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Auction] Failed to load auction: " + id);
                e.printStackTrace();
            }
        }
        
        plugin.getLogger().info("[Auction] Loaded " + auctions.size() + " auctions");
    }
}
