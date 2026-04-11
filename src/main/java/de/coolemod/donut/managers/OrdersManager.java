package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.storage.DataManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Einfaches Order-System: Käufer erstellt Order mit Preis/Anzahl, zahlt voraus.
 * Andere Spieler liefern Items und bekommen sofort Geld.
 */
public class OrdersManager {
    private final DonutPlugin plugin;
    private final DataManager data;
    private final Map<String, Order> orders = new HashMap<>();

    public java.util.Collection<Order> getAll() { return orders.values(); }

    public java.util.List<Order> getByOwner(UUID owner) {
        return orders.values().stream().filter(o -> o.owner.equals(owner)).collect(java.util.stream.Collectors.toList());
    }

    public boolean cancelOrder(UUID owner, String id) {
        Order o = orders.get(id);
        if (o == null || !o.owner.equals(owner)) return false;
        // Erstatte das verbleibende Geld
        double remaining = (o.requiredAmount - o.delivered) * o.pricePerItem;
        plugin.getEconomy().deposit(owner, remaining);
        orders.remove(id);
        save();
        return true;
    }

    public OrdersManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.data = new DataManager(plugin.getDataFolder(), plugin.getConfig().getString("data-file", "data.yml"));
        load();
    }

    public static class Order {
        public String id;
        public UUID owner;
        public ItemStack itemType; // exemplarisch
        public int requiredAmount;
        public double pricePerItem;
        public int delivered = 0;

        public Order(String id, UUID owner, ItemStack itemType, int requiredAmount, double pricePerItem) {
            this.id = id; this.owner = owner; this.itemType = itemType.clone(); this.requiredAmount = requiredAmount; this.pricePerItem = pricePerItem;
        }
    }

    public String createOrder(UUID owner, ItemStack itemType, int amount, double pricePerItem) {
        // Geld vom Owner reservieren
        double total = amount * pricePerItem;
        if (plugin.getEconomy().getBalance(owner) < total) return null;
        plugin.getEconomy().withdraw(owner, total);
        String id = UUID.randomUUID().toString();
        orders.put(id, new Order(id, owner, itemType, amount, pricePerItem));
        save();
        return id;
    }

    public boolean deliverToOrder(String id, UUID deliverer, int amount) {
        Order o = orders.get(id);
        if (o == null) return false;
        int canDeliver = Math.min(amount, o.requiredAmount - o.delivered);
        double pay = canDeliver * o.pricePerItem;
        plugin.getEconomy().deposit(deliverer, pay);
        o.delivered += canDeliver;
        if (o.delivered >= o.requiredAmount) orders.remove(id);
        save();
        return true;
    }

    public void save() {
        FileConfiguration cfg = data.getConfig();
        cfg.set("orders", null);
        for (Map.Entry<String, Order> e : orders.entrySet()) {
            String base = "orders." + e.getKey();
            cfg.set(base + ".owner", e.getValue().owner.toString());
            cfg.set(base + ".amount", e.getValue().requiredAmount);
            cfg.set(base + ".price", e.getValue().pricePerItem);
            cfg.set(base + ".delivered", e.getValue().delivered);
            cfg.set(base + ".item", e.getValue().itemType);
        }
        data.save();
    }

    private void load() {
        FileConfiguration cfg = data.getConfig();
        if (!cfg.isConfigurationSection("orders")) return;

        var ordersSection = cfg.getConfigurationSection("orders");
        if (ordersSection == null) {
            return;
        }

        for (String id : ordersSection.getKeys(false)) {
            try {
                String base = "orders." + id;
                String ownerValue = cfg.getString(base + ".owner");
                org.bukkit.inventory.ItemStack item = cfg.getItemStack(base + ".item");
                if (ownerValue == null || item == null) {
                    continue;
                }

                UUID owner = UUID.fromString(ownerValue);
                int amount = cfg.getInt(base + ".amount");
                double price = cfg.getDouble(base + ".price");
                int delivered = cfg.getInt(base + ".delivered");
                Order o = new Order(id, owner, item, amount, price);
                o.delivered = delivered;
                orders.put(id, o);
            } catch (Exception exception) {
                plugin.getLogger().warning("Überspringe defekten Order-Eintrag: " + id);
            }
        }
    }
}
