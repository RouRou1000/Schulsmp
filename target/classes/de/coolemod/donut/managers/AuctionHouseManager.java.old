package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.storage.DataManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Einfaches Auktionshaus: Items werden gelistet, bezahlt wird direkt an Verkäufer.
 * Persistente Speicherung in data.yml
 */
public class AuctionHouseManager {
    private final DonutPlugin plugin;
    private final DataManager data;

    // Auction ID -> Auction
    private final Map<String, Auction> auctions = new HashMap<>();

    public AuctionHouseManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.data = new DataManager(plugin.getDataFolder(), plugin.getConfig().getString("data-file", "data.yml"));
        load();
    }

    public static class Auction {
        public String id;
        public UUID seller;
        public ItemStack item;
        public double price;

        public Auction(String id, UUID seller, ItemStack item, double price) {
            this.id = id; this.seller = seller; this.item = item; this.price = price;
        }
    }

    public String listItem(UUID seller, ItemStack item, double price) {
        String id = UUID.randomUUID().toString();
        auctions.put(id, new Auction(id, seller, item.clone(), price));
        save();
        return id;
    }

    public boolean buy(org.bukkit.entity.Player buyer, String id) {
        Auction a = auctions.get(id);
        if (a == null) return false;
        double price = a.price;
        if (plugin.getEconomy().getBalance(buyer.getUniqueId()) < price) return false;
        if (!plugin.getEconomy().withdraw(buyer.getUniqueId(), price)) return false;
        // Zahlung an Verkäufer (auch wenn offline)
        plugin.getEconomy().deposit(a.seller, price);
        // Item an Käufer geben (Versuch Inventory, sonst drop)
        org.bukkit.inventory.ItemStack item = a.item.clone();
        if (buyer.getInventory().firstEmpty() == -1) buyer.getWorld().dropItemNaturally(buyer.getLocation(), item);
        else buyer.getInventory().addItem(item);
        // Verkäufer informieren, falls online
        org.bukkit.entity.Player seller = org.bukkit.Bukkit.getPlayer(a.seller);
        if (seller != null) seller.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§aDein Artikel wurde verkauft und du hast §e" + "%.2f".formatted(price) + "§a erhalten.");
        auctions.remove(id);
        save();
        return true;
    }

    public Collection<Auction> getAll() { return auctions.values(); }

    public java.util.List<Auction> getByOwner(UUID owner) {
        return auctions.values().stream().filter(a -> a.seller.equals(owner)).collect(java.util.stream.Collectors.toList());
    }

    public boolean cancel(UUID owner, String id) {
        Auction a = auctions.get(id);
        if (a == null || !a.seller.equals(owner)) return false;
        // Gib Item zurück (drop if inventory full)
        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(owner);
        if (p != null) {
            if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), a.item);
            else p.getInventory().addItem(a.item);
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§aAuktion zurückgezogen.");
        }
        auctions.remove(id);
        save();
        return true;
    }

    public void save() {
        FileConfiguration cfg = data.getConfig();
        cfg.set("auctions", null);
        for (Map.Entry<String, Auction> e : auctions.entrySet()) {
            String base = "auctions." + e.getKey();
            cfg.set(base + ".seller", e.getValue().seller.toString());
            cfg.set(base + ".price", e.getValue().price);
            cfg.set(base + ".item", e.getValue().item);
        }
        data.save();
    }

    private void load() {
        FileConfiguration cfg = data.getConfig();
        if (!cfg.contains("auctions")) return;
        for (String id : cfg.getConfigurationSection("auctions").getKeys(false)) {
            String base = "auctions." + id;
            UUID seller = UUID.fromString(cfg.getString(base + ".seller"));
            double price = cfg.getDouble(base + ".price");
            ItemStack item = cfg.getItemStack(base + ".item");
            Auction a = new Auction(id, seller, item, price);
            auctions.put(id, a);
        }
    }
}
