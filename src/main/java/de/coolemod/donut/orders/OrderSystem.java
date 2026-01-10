package de.coolemod.donut.orders;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Komplett neues Order-System mit UI
 */
public class OrderSystem {
    private final DonutPlugin plugin;
    private final Map<String, Order> orders = new HashMap<>();
    private final Map<UUID, CreateSession> createSessions = new HashMap<>();
    
    private final NamespacedKey UI_KEY;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey ORDER_ID_KEY;
    
    public OrderSystem(DonutPlugin plugin) {
        this.plugin = plugin;
        this.UI_KEY = new NamespacedKey(plugin, "order_ui");
        this.ACTION_KEY = new NamespacedKey(plugin, "order_action");
        this.ORDER_ID_KEY = new NamespacedKey(plugin, "order_id");
    }
    
    public String toSmallCaps(String text) {
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
    
    // ==================== ORDER MANAGEMENT ====================
    
    public static class Order {
        public String id;
        public UUID owner;
        public ItemStack itemType;
        public int requiredAmount;
        public double pricePerItem;
        public int delivered;
        public long timestamp;
        
        public Order(String id, UUID owner, ItemStack itemType, int requiredAmount, double pricePerItem) {
            this.id = id;
            this.owner = owner;
            this.itemType = itemType.clone();
            this.requiredAmount = requiredAmount;
            this.pricePerItem = pricePerItem;
            this.delivered = 0;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public String createOrder(UUID owner, ItemStack itemType, int amount, double pricePerItem) {
        double total = amount * pricePerItem;
        if (plugin.getEconomy().getBalance(owner) < total) return null;
        
        plugin.getEconomy().withdraw(owner, total);
        String id = UUID.randomUUID().toString();
        orders.put(id, new Order(id, owner, itemType, amount, pricePerItem));
        return id;
    }
    
    public boolean deliverToOrder(String orderId, Player deliverer, int amount) {
        Order order = orders.get(orderId);
        if (order == null) return false;
        if (order.owner.equals(deliverer.getUniqueId())) return false; // Can't deliver to own order
        
        int canDeliver = Math.min(amount, order.requiredAmount - order.delivered);
        if (canDeliver <= 0) return false;
        
        // Take items from player
        int toRemove = canDeliver;
        for (ItemStack item : deliverer.getInventory().getContents()) {
            if (item == null || !item.isSimilar(order.itemType)) continue;
            int take = Math.min(item.getAmount(), toRemove);
            item.setAmount(item.getAmount() - take);
            toRemove -= take;
            if (toRemove <= 0) break;
        }
        
        if (toRemove > 0) return false; // Not enough items
        
        // Pay deliverer
        double payment = canDeliver * order.pricePerItem;
        plugin.getEconomy().deposit(deliverer.getUniqueId(), payment);
        
        // Update order
        order.delivered += canDeliver;
        
        // Notify owner
        Player owner = Bukkit.getPlayer(order.owner);
        if (owner != null) {
            owner.sendMessage("§a✓ Deine Order wurde beliefert! " + canDeliver + "x von " + deliverer.getName());
        }
        
        // Complete order if fulfilled
        if (order.delivered >= order.requiredAmount) {
            orders.remove(orderId);
            if (owner != null) {
                owner.sendMessage("§a✓✓ Deine Order wurde vollständig erfüllt!");
            }
        }
        
        return true;
    }
    
    public boolean cancelOrder(Player player, String orderId) {
        Order order = orders.get(orderId);
        if (order == null) return false;
        if (!order.owner.equals(player.getUniqueId())) return false;
        
        // Refund remaining amount
        double remaining = (order.requiredAmount - order.delivered) * order.pricePerItem;
        plugin.getEconomy().deposit(player.getUniqueId(), remaining);
        orders.remove(orderId);
        
        player.sendMessage("§a✓ Order storniert. Rückerstattung: §e$" + String.format("%.2f", remaining));
        return true;
    }
    
    public List<Order> getOrders() {
        return new ArrayList<>(orders.values());
    }
    
    public List<Order> getPlayerOrders(UUID player) {
        List<Order> result = new ArrayList<>();
        for (Order o : orders.values()) {
            if (o.owner.equals(player)) result.add(o);
        }
        return result;
    }
    
    // ==================== CREATE SESSION ====================
    
    public static class CreateSession {
        public ItemStack item;
        public int amount;
        public double price;
        public boolean amountSet;
        public boolean priceSet;
        
        public CreateSession() {
            this.item = null;
            this.amount = 0;
            this.price = 0;
            this.amountSet = false;
            this.priceSet = false;
        }
    }
    
    public CreateSession getCreateSession(UUID player) {
        return createSessions.get(player);
    }
    
    public void startCreateSession(UUID player) {
        createSessions.put(player, new CreateSession());
    }
    
    public void endCreateSession(UUID player) {
        CreateSession session = createSessions.remove(player);
        if (session != null && session.item != null) {
            Player p = Bukkit.getPlayer(player);
            if (p != null) {
                p.getInventory().addItem(session.item);
            }
        }
    }
    
    // ==================== GUI CREATION ====================
    
    public Inventory createBrowseGUI(int page) {
        Inventory inv = Bukkit.createInventory(null, 54, "§9§l" + toSmallCaps("ORDERS") + " §8(" + (page + 1) + ")");
        
        // Fill borders
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName("§8⬛");
        border.setItemMeta(meta);
        
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,51,52}) {
            inv.setItem(i, border);
        }
        
        // Orders (28 slots: 10-16, 19-25, 28-34, 37-43)
        List<Order> all = getOrders();
        int start = page * 28;
        int end = Math.min(start + 28, all.size());
        
        int slot = 10;
        for (int i = start; i < end; i++) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            
            Order order = all.get(i);
            inv.setItem(slot, createOrderItem(order));
            slot++;
        }
        
        // Navigation
        ItemStack navInfo = mark(new ItemStack(Material.PAPER), "disabled", null);
        ItemMeta navMeta = navInfo.getItemMeta();
        navMeta.setDisplayName("§e§l📄 " + toSmallCaps("SEITE") + " §f" + (page + 1));
        List<String> navLore = new ArrayList<>();
        navLore.add("§8");
        navLore.add("§7Orders: §f" + all.size());
        navLore.add("§7Seiten: §f" + ((all.size() + 27) / 28));
        navLore.add("§8");
        if (page > 0) navLore.add("§a« Vorherige Seite (Pfeil links)");
        if (end < all.size()) navLore.add("§a» Nächste Seite (Pfeil rechts)");
        navMeta.setLore(navLore);
        navInfo.setItemMeta(navMeta);
        inv.setItem(49, navInfo);
        
        // My Orders Button (Slot 50)
        ItemStack myBtn = mark(new ItemStack(Material.ENDER_CHEST), "my_orders", null);
        ItemMeta myMeta = myBtn.getItemMeta();
        myMeta.setDisplayName("§e§l⚑ " + toSmallCaps("MEINE ORDERS"));
        List<String> myLore = new ArrayList<>();
        myLore.add("§8");
        myLore.add("§7Zeige deine aktiven");
        myLore.add("§7Orders an");
        myLore.add("§8");
        myLore.add("§e▸ Klicken zum Öffnen");
        myMeta.setLore(myLore);
        myBtn.setItemMeta(myMeta);
        inv.setItem(50, myBtn);
        
        return inv;
    }
    
    public Inventory createMyOrdersGUI(UUID player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§9§l" + toSmallCaps("MEINE ORDERS"));
        
        // Fill borders
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName("§8⬛");
        border.setItemMeta(meta);
        
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53}) {
            inv.setItem(i, border);
        }
        
        // First slot: "Neue Order erstellen" Button
        ItemStack newBtn = mark(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "new", null);
        ItemMeta newMeta = newBtn.getItemMeta();
        newMeta.setDisplayName("§a§l+ " + toSmallCaps("NEUE ORDER"));
        List<String> newLore = new ArrayList<>();
        newLore.add("§8");
        newLore.add("§7Erstelle eine Kauforder");
        newLore.add("§7für ein Item");
        newLore.add("§8");
        newLore.add("§e▸ Klicken zum Erstellen");
        newMeta.setLore(newLore);
        newBtn.setItemMeta(newMeta);
        inv.setItem(10, newBtn);
        
        // My Orders (starting from slot 11)
        List<Order> mine = getPlayerOrders(player);
        int slot = 11;
        for (Order order : mine) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            if (slot >= 44) break;
            
            ItemStack display = order.itemType.clone();
            ItemMeta displayMeta = display.getItemMeta();
            List<String> lore = displayMeta.hasLore() ? new ArrayList<>(displayMeta.getLore()) : new ArrayList<>();
            lore.add("§8");
            lore.add("§8§m                    ");
            lore.add("§7Benötigt: §f" + order.requiredAmount + "x");
            lore.add("§7Geliefert: §a" + order.delivered + "x");
            lore.add("§7Verbleibend: §e" + (order.requiredAmount - order.delivered) + "x");
            lore.add("§8");
            lore.add("§6⛃ §7Preis/Stück: §e$" + String.format("%.2f", order.pricePerItem));
            lore.add("§6⛃ §7Gesamt: §e$" + String.format("%.2f", order.requiredAmount * order.pricePerItem));
            lore.add("§8§m                    ");
            lore.add("§8");
            lore.add("§c§l✖ " + toSmallCaps("STORNIEREN"));
            lore.add("§7Verbleibendes Geld wird erstattet");
            displayMeta.setLore(lore);
            display.setItemMeta(displayMeta);
            mark(display, "cancel", order.id);
            
            inv.setItem(slot, display);
            slot++;
        }
        
        return inv;
    }
    
    public Inventory createNewOrderGUI(UUID player) {
        CreateSession session = createSessions.get(player);
        if (session == null) {
            session = new CreateSession();
            createSessions.put(player, session);
        }
        
        Inventory inv = Bukkit.createInventory(null, 9, "§a§l" + toSmallCaps("NEUE ORDER"));
        
        // Slot 4: Item without modification (free chest slot)
        if (session.item != null) {
            inv.setItem(4, session.item.clone());
        }
        // Slot 4 bleibt leer wenn kein Item
        
        // Slot 2: Back Button
        ItemStack back = mark(new ItemStack(Material.RED_STAINED_GLASS_PANE), "back", null);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§c§l« " + toSmallCaps("ZURUCK"));
        List<String> backLore = new ArrayList<>();
        backLore.add("§8");
        backLore.add("§7Abbrechen und Item");
        backLore.add("§7zurückerhalten");
        backLore.add("§8");
        backLore.add("§c▸ Klicken zum Abbrechen");
        backMeta.setLore(backLore);
        back.setItemMeta(backMeta);
        inv.setItem(2, back);
        
        // Slot 5: Set Amount Button
        if (session.item != null) {
            ItemStack amountBtn = mark(new ItemStack(Material.IRON_INGOT), "set_amount", null);
            ItemMeta amountMeta = amountBtn.getItemMeta();
            if (session.amountSet) {
                amountMeta.setDisplayName("§b§l# " + toSmallCaps("MENGE ÄNDERN"));
            } else {
                amountMeta.setDisplayName("§b§l# " + toSmallCaps("MENGE FESTLEGEN"));
            }
            List<String> amountLore = new ArrayList<>();
            amountLore.add("§8");
            if (session.amountSet) {
                amountLore.add("§7Aktuelle Menge: §f" + session.amount);
                amountLore.add("§8");
            }
            amountLore.add("§7Öffnet ein Schild zur");
            amountLore.add("§7Eingabe der Menge");
            amountLore.add("§8");
            amountLore.add("§e▸ Klicken zum Öffnen");
            amountMeta.setLore(amountLore);
            amountBtn.setItemMeta(amountMeta);
            inv.setItem(5, amountBtn);
        } else {
            ItemStack disabled = mark(new ItemStack(Material.GRAY_DYE), "disabled", null);
            ItemMeta disabledMeta = disabled.getItemMeta();
            disabledMeta.setDisplayName("§7§l# " + toSmallCaps("MENGE FESTLEGEN"));
            List<String> disabledLore = new ArrayList<>();
            disabledLore.add("§8");
            disabledLore.add("§c✖ Zuerst Item platzieren!");
            disabledMeta.setLore(disabledLore);
            disabled.setItemMeta(disabledMeta);
            inv.setItem(5, disabled);
        }
        
        // Slot 6: Set Price Button
        if (session.item != null && session.amountSet) {
            ItemStack priceBtn = mark(new ItemStack(Material.GOLD_INGOT), "set_price", null);
            ItemMeta priceMeta = priceBtn.getItemMeta();
            if (session.priceSet) {
                priceMeta.setDisplayName("§6§l$ " + toSmallCaps("PREIS ÄNDERN"));
            } else {
                priceMeta.setDisplayName("§6§l$ " + toSmallCaps("PREIS FESTLEGEN"));
            }
            List<String> priceLore = new ArrayList<>();
            priceLore.add("§8");
            if (session.priceSet) {
                priceLore.add("§7Aktueller Preis: §e$" + String.format("%.2f", session.price) + "/Stück");
                priceLore.add("§8");
            }
            priceLore.add("§7Öffnet ein Schild zur");
            priceLore.add("§7Eingabe des Preises/Stück");
            priceLore.add("§8");
            priceLore.add("§e▸ Klicken zum Öffnen");
            priceMeta.setLore(priceLore);
            priceBtn.setItemMeta(priceMeta);
            inv.setItem(6, priceBtn);
        } else if (!session.amountSet) {
            ItemStack disabled = mark(new ItemStack(Material.GRAY_DYE), "disabled", null);
            ItemMeta disabledMeta = disabled.getItemMeta();
            disabledMeta.setDisplayName("§7§l$ " + toSmallCaps("PREIS FESTLEGEN"));
            List<String> disabledLore = new ArrayList<>();
            disabledLore.add("§8");
            disabledLore.add("§c✖ Zuerst Menge festlegen!");
            disabledMeta.setLore(disabledLore);
            disabled.setItemMeta(disabledMeta);
            inv.setItem(6, disabled);
        }
        
        // Slot 8: Confirm Button
        if (session.priceSet && session.amountSet && session.item != null) {
            double total = session.amount * session.price;
            ItemStack confirm = mark(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "confirm", null);
            ItemMeta confirmMeta = confirm.getItemMeta();
            confirmMeta.setDisplayName("§a§l✓ " + toSmallCaps("ORDER ERSTELLEN"));
            List<String> confirmLore = new ArrayList<>();
            confirmLore.add("§8");
            confirmLore.add("§7Item: §f" + session.item.getType().name());
            confirmLore.add("§7Menge: §f" + session.amount + "x");
            confirmLore.add("§7Preis/Stück: §e$" + String.format("%.2f", session.price));
            confirmLore.add("§8");
            confirmLore.add("§6⛃ §7Gesamt: §e§l$" + String.format("%.2f", total));
            confirmLore.add("§8");
            confirmLore.add("§7Das Geld wird sofort reserviert");
            confirmLore.add("§8");
            confirmLore.add("§a▸ Klicken zum Erstellen");
            confirmMeta.setLore(confirmLore);
            confirm.setItemMeta(confirmMeta);
            inv.setItem(8, confirm);
        }
        
        return inv;
    }
    
    private ItemStack createOrderItem(Order order) {
        ItemStack display = order.itemType.clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("§8");
        lore.add("§8§m                    ");
        lore.add("§b⚑ §7Käufer: §f" + Bukkit.getOfflinePlayer(order.owner).getName());
        lore.add("§8");
        lore.add("§7Benötigt: §f" + order.requiredAmount + "x");
        lore.add("§7Geliefert: §a" + order.delivered + "x");
        lore.add("§7Verbleibend: §e" + (order.requiredAmount - order.delivered) + "x");
        lore.add("§8");
        lore.add("§6⛃ §7Preis/Stück: §e$" + String.format("%.2f", order.pricePerItem));
        lore.add("§8§m                    ");
        lore.add("§8");
        lore.add("§a§l» " + toSmallCaps("KLICKEN ZUM BELIEFERN"));
        lore.add("§7Items aus deinem Inventar");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return mark(display, "deliver", order.id);
    }
    
    private ItemStack mark(ItemStack item, String action, String id) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(UI_KEY, PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        if (id != null) {
            meta.getPersistentDataContainer().set(ORDER_ID_KEY, PersistentDataType.STRING, id);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    public boolean isOrderGUI(String title) {
        return title != null && (
            title.contains("ᴏʀᴅᴇʀs") ||
            title.contains("ᴍᴇɪɴᴇ ᴏʀᴅᴇʀs") ||
            title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ")
        );
    }
    
    public String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
    }
    
    public String getOrderId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ORDER_ID_KEY, PersistentDataType.STRING);
    }
}
