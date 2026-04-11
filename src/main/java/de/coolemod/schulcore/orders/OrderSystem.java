package de.coolemod.schulcore.orders;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.utils.NumberFormatter;
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
    private final SchulCorePlugin plugin;
    private final Map<String, Order> orders = new HashMap<>();
    private final Map<UUID, CreateSession> createSessions = new HashMap<>();
    private final Map<UUID, BrowseSession> browseSessions = new HashMap<>();
    private final Map<UUID, DeliverSession> deliverSessions = new HashMap<>();
    
    private final NamespacedKey UI_KEY;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey ORDER_ID_KEY;
    
    public OrderSystem(SchulCorePlugin plugin) {
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
        
        player.sendMessage("§a✓ Order storniert. Rückerstattung: §e" + NumberFormatter.formatMoney(remaining));
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
    
    // ==================== BROWSE SESSION (SEARCH & SORT) ====================
    
    public enum SortMode {
        NEWEST("ɴᴇᴜᴇsᴛᴇ", "§7Neuste zuerst"),
        OLDEST("äʟᴛᴇsᴛᴇ", "§7Älteste zuerst"),
        PRICE_LOW("ᴘʀᴇɪs ↑", "§7Preis aufsteigend"),
        PRICE_HIGH("ᴘʀᴇɪs ↓", "§7Preis absteigend"),
        PRICE_PER_ITEM_HIGH("§/sᴛüᴄᴋ ↓", "§7Höchster Preis/Stück"),
        NAME("ɴᴀᴍᴇ", "§7Alphabetisch");
        
        public final String display;
        public final String desc;
        
        SortMode(String display, String desc) {
            this.display = display;
            this.desc = desc;
        }
        
        public SortMode next() {
            int ord = this.ordinal();
            SortMode[] values = values();
            return values[(ord + 1) % values.length];
        }
    }
    
    public static class BrowseSession {
        public String searchQuery;
        public SortMode sortMode;
        
        public BrowseSession() {
            this.searchQuery = null;
            this.sortMode = SortMode.NEWEST;
        }
    }
    
    public BrowseSession getBrowseSession(UUID player) {
        return browseSessions.computeIfAbsent(player, k -> new BrowseSession());
    }
    
    public void clearBrowseSession(UUID player) {
        browseSessions.remove(player);
    }
    
    public List<Order> getFilteredOrders(UUID player) {
        BrowseSession session = getBrowseSession(player);
        List<Order> result = new ArrayList<>(orders.values());
        
        // Apply search filter
        if (session.searchQuery != null && !session.searchQuery.trim().isEmpty()) {
            String query = session.searchQuery.toLowerCase().trim();
            result.removeIf(order -> {
                if (order == null || order.itemType == null) return true;
                String itemName = order.itemType.getType().name().toLowerCase().replace("_", " ");
                return !itemName.contains(query);
            });
        }
        
        // Apply sorting
        switch (session.sortMode) {
            case NEWEST:
                result.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
                break;
            case OLDEST:
                result.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
                break;
            case PRICE_LOW:
                result.sort((a, b) -> Double.compare(
                    a.pricePerItem * (a.requiredAmount - a.delivered),
                    b.pricePerItem * (b.requiredAmount - b.delivered)));
                break;
            case PRICE_HIGH:
                result.sort((a, b) -> Double.compare(
                    b.pricePerItem * (b.requiredAmount - b.delivered),
                    a.pricePerItem * (a.requiredAmount - a.delivered)));
                break;
            case PRICE_PER_ITEM_HIGH:
                result.sort((a, b) -> Double.compare(b.pricePerItem, a.pricePerItem));
                break;
            case NAME:
                result.sort((a, b) -> {
                    if (a == null || a.itemType == null) return 1;
                    if (b == null || b.itemType == null) return -1;
                    return a.itemType.getType().name().compareTo(b.itemType.getType().name());
                });
                break;
        }
        
        return result;
    }
    
    // ==================== GUI CREATION ====================
    
    public Inventory createBrowseGUI(int page) {
        return createBrowseGUI(page, null);
    }
    
    public Inventory createBrowseGUI(int page, UUID player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§9§l" + toSmallCaps("ORDERS") + " §8(" + (page + 1) + ")");
        
        // Fill borders
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName("§8⬛");
        border.setItemMeta(meta);
        
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,51,52}) {
            inv.setItem(i, border);
        }
        
        // Orders (filtered if player is provided)
        List<Order> all = (player != null) ? getFilteredOrders(player) : getOrders();
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
        
        // Search Button (Slot 48)
        ItemStack searchBtn = mark(new ItemStack(Material.COMPASS), "search", null);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        searchMeta.setDisplayName("§e§l🔍 " + toSmallCaps("SUCHEN"));
        List<String> searchLore = new ArrayList<>();
        searchLore.add("§8");
        if (player != null) {
            BrowseSession session = getBrowseSession(player);
            if (session.searchQuery != null && !session.searchQuery.isEmpty()) {
                searchLore.add("§7Aktive Suche:");
                searchLore.add("§f\"" + session.searchQuery + "\"");
                searchLore.add("§8");
                searchLore.add("§a§lLinksklick §8- §7Neue Suche");
                searchLore.add("§c§lRechtsklick §8- §7Suche löschen");
            } else {
                searchLore.add("§7Durchsuche Orders");
                searchLore.add("§7nach Item-Namen");
                searchLore.add("§8");
                searchLore.add("§e▸ Klicken zum Suchen");
            }
        } else {
            searchLore.add("§7Durchsuche Orders");
            searchLore.add("§7nach Item-Namen");
            searchLore.add("§8");
            searchLore.add("§e▸ Klicken zum Suchen");
        }
        searchMeta.setLore(searchLore);
        searchBtn.setItemMeta(searchMeta);
        inv.setItem(48, searchBtn);
        
        // Sort Button (Slot 50)
        ItemStack sortBtn = mark(new ItemStack(Material.HOPPER), "sort", null);
        ItemMeta sortMeta = sortBtn.getItemMeta();
        if (player != null) {
            BrowseSession session = getBrowseSession(player);
            sortMeta.setDisplayName("§6§l⇅ " + session.sortMode.display);
        } else {
            sortMeta.setDisplayName("§6§l⇅ " + toSmallCaps("SORTIERUNG"));
        }
        List<String> sortLore = new ArrayList<>();
        sortLore.add("§8");
        if (player != null) {
            BrowseSession session = getBrowseSession(player);
            sortLore.add("§7Aktuell: §e" + session.sortMode.display);
            sortLore.add("§8" + session.sortMode.desc);
            sortLore.add("§8");
            sortLore.add("§e▸ Klicken zum Wechseln");
        } else {
            sortLore.add("§7Sortiere Orders");
            sortLore.add("§e▸ Klicken zum Sortieren");
        }
        sortMeta.setLore(sortLore);
        sortBtn.setItemMeta(sortMeta);
        inv.setItem(50, sortBtn);
        
        // Navigation (Slot 49)
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
        
        // My Orders Button (Slot 53)
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
        inv.setItem(53, myBtn);
        
        // Previous Page Button (Slot 16)
        if (page > 0) {
            ItemStack prev = mark(new ItemStack(Material.ARROW), "prev", String.valueOf(page - 1));
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName("§a§l◀ " + toSmallCaps("ZURÜCK"));
            List<String> prevLore = new ArrayList<>();
            prevLore.add("§8");
            prevLore.add("§7Gehe zu Seite §f" + page);
            prevLore.add("§8");
            prevLore.add("§e▸ Klicken zum Wechseln");
            prevMeta.setLore(prevLore);
            prev.setItemMeta(prevMeta);
            inv.setItem(16, prev);
        }
        
        // Next Page Button (Slot 24)
        if (end < all.size()) {
            ItemStack next = mark(new ItemStack(Material.ARROW), "next", String.valueOf(page + 1));
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("§a§l▶ " + toSmallCaps("WEITER"));
            List<String> nextLore = new ArrayList<>();
            nextLore.add("§8");
            nextLore.add("§7Gehe zu Seite §f" + (page + 2));
            nextLore.add("§8");
            nextLore.add("§e▸ Klicken zum Wechseln");
            nextMeta.setLore(nextLore);
            next.setItemMeta(nextMeta);
            inv.setItem(24, next);
        }
        
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
            lore.add("§6⛃ §7Preis/Stück: §e" + NumberFormatter.formatMoney(order.pricePerItem));
            lore.add("§6⛃ §7Gesamt: §e" + NumberFormatter.formatMoney(order.requiredAmount * order.pricePerItem));
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
        
        // Fill empty slots with border so shift-click goes to slot 4
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("§8⬛");
        border.setItemMeta(borderMeta);
        for (int i : new int[]{0, 1, 3, 7}) {
            inv.setItem(i, border.clone());
        }
        
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
            amountLore.add("§7Starte Chat-Eingabe für");
            amountLore.add("§7die gewünschte Menge");
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
                priceLore.add("§7Aktueller Preis: §e" + NumberFormatter.formatMoney(session.price) + "/Stück");
                priceLore.add("§8");
            }
            priceLore.add("§7Starte Chat-Eingabe für");
            priceLore.add("§7Preis pro Stück");
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
    
    public void updateNewOrderButtons(Inventory inv, CreateSession session) {
        // Slot 5: Amount button
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
            amountLore.add("§7Starte Chat-Eingabe für");
            amountLore.add("§7die gewünschte Menge");
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
        
        // Slot 6: Price button
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
                priceLore.add("§7Aktueller Preis: §e" + NumberFormatter.formatMoney(session.price) + "/Stück");
                priceLore.add("§8");
            }
            priceLore.add("§7Starte Chat-Eingabe für");
            priceLore.add("§7Preis pro Stück");
            priceLore.add("§8");
            priceLore.add("§e▸ Klicken zum Öffnen");
            priceMeta.setLore(priceLore);
            priceBtn.setItemMeta(priceMeta);
            inv.setItem(6, priceBtn);
        } else {
            ItemStack disabled = mark(new ItemStack(Material.GRAY_DYE), "disabled", null);
            ItemMeta disabledMeta = disabled.getItemMeta();
            disabledMeta.setDisplayName("§7§l$ " + toSmallCaps("PREIS FESTLEGEN"));
            List<String> disabledLore = new ArrayList<>();
            disabledLore.add("§8");
            if (session.item == null) {
                disabledLore.add("§c✖ Zuerst Item platzieren!");
            } else {
                disabledLore.add("§c✖ Zuerst Menge festlegen!");
            }
            disabledMeta.setLore(disabledLore);
            disabled.setItemMeta(disabledMeta);
            inv.setItem(6, disabled);
        }
        
        // Slot 8: Confirm button
        if (session.priceSet && session.amountSet && session.item != null) {
            double total = session.amount * session.price;
            ItemStack confirm = mark(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "confirm", null);
            ItemMeta confirmMeta = confirm.getItemMeta();
            confirmMeta.setDisplayName("§a§l✓ " + toSmallCaps("ORDER ERSTELLEN"));
            List<String> confirmLore = new ArrayList<>();
            confirmLore.add("§8");
            confirmLore.add("§7Item: §f" + session.item.getType().name());
            confirmLore.add("§7Menge: §f" + session.amount + "x");
            confirmLore.add("§7Preis/Stück: §e" + NumberFormatter.formatMoney(session.price));
            confirmLore.add("§8");
            confirmLore.add("§6⛃ §7Gesamt: §e§l" + NumberFormatter.formatMoney(total));
            confirmLore.add("§8");
            confirmLore.add("§7Das Geld wird sofort reserviert");
            confirmLore.add("§8");
            confirmLore.add("§a▸ Klicken zum Erstellen");
            confirmMeta.setLore(confirmLore);
            confirm.setItemMeta(confirmMeta);
            inv.setItem(8, confirm);
        } else {
            inv.setItem(8, null);
        }
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
        lore.add("§6⛃ §7Preis/Stück: §e" + NumberFormatter.formatMoney(order.pricePerItem));
        lore.add("§8§m                    ");
        lore.add("§8");
        lore.add("§a§l» " + toSmallCaps("KLICKEN ZUM BELIEFERN"));
        lore.add("§7Öffnet ein Lieferfenster");
        meta.setLore(lore);
        display.setItemMeta(meta);
        return mark(display, "deliver", order.id);
    }
    
    // ==================== DELIVER SESSION ====================
    
    public static class DeliverSession {
        public String orderId;
        public DeliverSession(String orderId) {
            this.orderId = orderId;
        }
    }
    
    public DeliverSession getDeliverSession(UUID player) {
        return deliverSessions.get(player);
    }
    
    public void startDeliverSession(UUID player, String orderId) {
        deliverSessions.put(player, new DeliverSession(orderId));
    }
    
    public void endDeliverSession(UUID player) {
        deliverSessions.remove(player);
    }
    
    public Order getOrder(String orderId) {
        return orders.get(orderId);
    }
    
    public Inventory createDeliverGUI(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) return null;
        
        String itemName = order.itemType.getType().name().replace("_", " ");
        Inventory inv = Bukkit.createInventory(null, 54, "§9§l" + toSmallCaps("ORDER LIEFERN"));
        
        int remaining = order.requiredAmount - order.delivered;
        
        // Obere Reihe: Info-Leiste
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("§8⬛");
        border.setItemMeta(borderMeta);
        for (int i : new int[]{0,1,2,3,5,6,7,8}) {
            inv.setItem(i, border);
        }
        
        // Slot 4: Anzeige welches Item benötigt wird
        ItemStack infoItem = order.itemType.clone();
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§e§l⚑ " + toSmallCaps("GESUCHTES ITEM"));
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8");
        infoLore.add("§7Item: §f" + itemName);
        infoLore.add("§7Verbleibend: §e" + remaining + "x");
        infoLore.add("§6⛃ §7Preis/Stück: §e" + NumberFormatter.formatMoney(order.pricePerItem));
        infoLore.add("§8");
        infoLore.add("§7Lege die passenden Items");
        infoLore.add("§7in die freien Slots unten.");
        infoLore.add("§7Falsche Items werden zurückgegeben.");
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        mark(infoItem, "info_display", null);
        inv.setItem(4, infoItem);
        
        // Slots 9-44: Freie Slots für Items (36 freie Plätze)
        // Bleiben leer - Spieler kann Items reinlegen
        
        // Untere Reihe: Aktionsleiste
        for (int i : new int[]{45,46,47,51,52}) {
            inv.setItem(i, border);
        }
        
        // Slot 48: Zurück-Button
        ItemStack back = mark(new ItemStack(Material.RED_STAINED_GLASS_PANE), "deliver_back", null);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§c§l« " + toSmallCaps("ABBRECHEN"));
        List<String> backLore = new ArrayList<>();
        backLore.add("§8");
        backLore.add("§7Items zurückerhalten");
        backLore.add("§7und zum Browse zurück");
        backMeta.setLore(backLore);
        back.setItemMeta(backMeta);
        inv.setItem(48, back);
        
        // Slot 49: Live-Info (wird beim Klicken aktualisiert)
        ItemStack payInfo = mark(new ItemStack(Material.GOLD_BLOCK), "deliver_info", null);
        ItemMeta payMeta = payInfo.getItemMeta();
        payMeta.setDisplayName("§6§l⛃ " + toSmallCaps("AUSZAHLUNG"));
        List<String> payLore = new ArrayList<>();
        payLore.add("§8");
        payLore.add("§7Passende Items: §f0x");
        payLore.add("§7Auszahlung: §e$0.00");
        payLore.add("§8");
        payLore.add("§7Lege Items ein um");
        payLore.add("§7die Auszahlung zu sehen");
        payMeta.setLore(payLore);
        payInfo.setItemMeta(payMeta);
        inv.setItem(49, payInfo);
        
        // Slot 53: Bestätigen-Button
        ItemStack confirm = mark(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "deliver_confirm", null);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§l✓ " + toSmallCaps("LIEFERN"));
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("§8");
        confirmLore.add("§7Bestätige die Lieferung");
        confirmLore.add("§7und erhalte die Bezahlung");
        confirmMeta.setLore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        inv.setItem(53, confirm);
        
        return inv;
    }
    
    /** Aktualisiert die Auszahlungs-Info im Deliver-GUI */
    public void updateDeliverInfo(Inventory inv, String orderId) {
        Order order = orders.get(orderId);
        if (order == null) return;
        
        int remaining = order.requiredAmount - order.delivered;
        int matchingCount = 0;
        int nonMatchingCount = 0;
        
        for (int i = 9; i <= 44; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            if (item.isSimilar(order.itemType)) {
                matchingCount += item.getAmount();
            } else {
                nonMatchingCount += item.getAmount();
            }
        }
        
        int canDeliver = Math.min(matchingCount, remaining);
        double payment = canDeliver * order.pricePerItem;
        
        // Slot 49: Auszahlungs-Info aktualisieren
        ItemStack payInfo = mark(new ItemStack(Material.GOLD_BLOCK), "deliver_info", null);
        ItemMeta payMeta = payInfo.getItemMeta();
        payMeta.setDisplayName("§6§l⛃ " + toSmallCaps("AUSZAHLUNG"));
        List<String> payLore = new ArrayList<>();
        payLore.add("§8");
        payLore.add("§7Passende Items: §a" + matchingCount + "x");
        if (matchingCount > remaining) {
            payLore.add("§7Wird geliefert: §e" + canDeliver + "x §7(max " + remaining + ")");
            payLore.add("§7Überschuss: §c" + (matchingCount - remaining) + "x §7(zurück)");
        } else {
            payLore.add("§7Wird geliefert: §e" + canDeliver + "x");
        }
        if (nonMatchingCount > 0) {
            payLore.add("§7Falsche Items: §c" + nonMatchingCount + "x §7(werden zurückgegeben)");
        }
        payLore.add("§8");
        payLore.add("§6⛃ §7Auszahlung: §a§l" + NumberFormatter.formatMoney(payment));
        payMeta.setLore(payLore);
        payInfo.setItemMeta(payMeta);
        inv.setItem(49, payInfo);
    }
    
    /** Verarbeitet die Lieferung: nimmt passende Items, gibt Rest zurück */
    public DeliverResult processDelivery(Player player, Inventory deliverInv, String orderId) {
        Order order = orders.get(orderId);
        if (order == null) return new DeliverResult(false, 0, 0);
        
        int remaining = order.requiredAmount - order.delivered;
        int matchingCount = 0;
        List<ItemStack> matchingItems = new ArrayList<>();
        List<ItemStack> nonMatchingItems = new ArrayList<>();
        
        // Alle Items aus der Deliver-GUI sammeln
        for (int i = 9; i <= 44; i++) {
            ItemStack item = deliverInv.getItem(i);
            if (item == null || item.getType().isAir()) continue;
            if (item.isSimilar(order.itemType)) {
                matchingCount += item.getAmount();
                matchingItems.add(item.clone());
            } else {
                nonMatchingItems.add(item.clone());
            }
            deliverInv.setItem(i, null); // Slot leeren
        }
        
        if (matchingCount <= 0) return new DeliverResult(false, 0, 0);
        
        int canDeliver = Math.min(matchingCount, remaining);
        double payment = canDeliver * order.pricePerItem;
        
        // Items verbrauchen (nur so viele wie nötig)
        int consumed = 0;
        for (ItemStack item : matchingItems) {
            if (consumed >= canDeliver) {
                // Überschüssige passende Items zurückgeben
                player.getInventory().addItem(item);
                continue;
            }
            int take = Math.min(item.getAmount(), canDeliver - consumed);
            consumed += take;
            if (take < item.getAmount()) {
                // Teilweise verbraucht - Rest zurückgeben
                ItemStack leftover = item.clone();
                leftover.setAmount(item.getAmount() - take);
                player.getInventory().addItem(leftover);
            }
        }
        
        // Nicht-passende Items zurückgeben
        for (ItemStack item : nonMatchingItems) {
            HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            // Falls Inventar voll: auf den Boden droppen
            overflow.values().forEach(i -> player.getWorld().dropItem(player.getLocation(), i));
        }
        
        // Bezahlen & Order aktualisieren
        plugin.getEconomy().deposit(player.getUniqueId(), payment);
        order.delivered += canDeliver;
        
        // Owner benachrichtigen
        Player owner = Bukkit.getPlayer(order.owner);
        if (owner != null) {
            owner.sendMessage("§a✓ Deine Order wurde beliefert! " + canDeliver + "x von " + player.getName());
        }
        
        if (order.delivered >= order.requiredAmount) {
            orders.remove(orderId);
            if (owner != null) {
                owner.sendMessage("§a✓✓ Deine Order wurde vollständig erfüllt!");
            }
        }
        
        return new DeliverResult(true, canDeliver, payment);
    }
    
    public record DeliverResult(boolean success, int delivered, double payment) {}
    
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
            title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ") ||
            title.contains("ᴏʀᴅᴇʀ ʟɪᴇғᴇʀɴ")
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
