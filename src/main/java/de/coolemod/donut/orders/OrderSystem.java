package de.coolemod.donut.orders;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Komplett neues Order-System mit UI
 */
public class OrderSystem {
    private final DonutPlugin plugin;
    private final Map<String, Order> orders = new HashMap<>();
    private final Map<UUID, CreateSession> createSessions = new HashMap<>();
    private final Map<UUID, BrowseSession> browseSessions = new HashMap<>();
    private final Map<UUID, DeliverySession> deliverySessions = new HashMap<>();
    private final Map<String, List<ItemStack>> pendingCollections = new HashMap<>();

    private final NamespacedKey UI_KEY;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey ORDER_ID_KEY;
    private final NamespacedKey MATERIAL_KEY;

    private static final int ITEMS_PER_PAGE = 28;

    private static final Set<String> UNOBTAINABLE = Set.of(
        "BARRIER", "STRUCTURE_BLOCK", "STRUCTURE_VOID", "JIGSAW", "LIGHT",
        "DEBUG_STICK", "KNOWLEDGE_BOOK", "BEDROCK", "PETRIFIED_OAK_SLAB",
        "REINFORCED_DEEPSLATE", "FROGSPAWN", "FROSTED_ICE", "SPAWNER",
        "BUDDING_AMETHYST", "END_PORTAL_FRAME", "TRIAL_SPAWNER", "VAULT",
        "FARMLAND", "PLAYER_HEAD", "PLAYER_WALL_HEAD", "COMMAND_BLOCK_MINECART"
    );

    public OrderSystem(DonutPlugin plugin) {
        this.plugin = plugin;
        this.UI_KEY = new NamespacedKey(plugin, "order_ui");
        this.ACTION_KEY = new NamespacedKey(plugin, "order_action");
        this.ORDER_ID_KEY = new NamespacedKey(plugin, "order_id");
        this.MATERIAL_KEY = new NamespacedKey(plugin, "order_material");
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
        public boolean closed;
        public boolean cancelled;

        public Order(String id, UUID owner, ItemStack itemType, int requiredAmount, double pricePerItem) {
            this.id = id;
            this.owner = owner;
            this.itemType = itemType.clone();
            this.requiredAmount = requiredAmount;
            this.pricePerItem = pricePerItem;
            this.delivered = 0;
            this.timestamp = System.currentTimeMillis();
            this.closed = false;
            this.cancelled = false;
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

    public boolean completeDelivery(String orderId, Player deliverer, int count) {
        Order order = orders.get(orderId);
        if (order == null || order.closed) return false;

        int canDeliver = Math.min(count, order.requiredAmount - order.delivered);
        if (canDeliver <= 0) return false;

        // Pay deliverer
        double payment = canDeliver * order.pricePerItem;
        plugin.getEconomy().deposit(deliverer.getUniqueId(), payment);

        // Store delivered items for buyer to collect
        DeliverySession session = getDeliverySession(deliverer.getUniqueId());
        if (session != null && session.matchingItems != null) {
            List<ItemStack> toStore = new ArrayList<>();
            int remaining = canDeliver;
            for (ItemStack item : session.matchingItems) {
                if (remaining <= 0) break;
                ItemStack stored = item.clone();
                int take = Math.min(stored.getAmount(), remaining);
                stored.setAmount(take);
                toStore.add(stored);
                remaining -= take;
            }
            addPendingItems(order.id, toStore);
        }

        // Update order
        order.delivered += canDeliver;

        // Notify owner
        Player owner = Bukkit.getPlayer(order.owner);
        if (owner != null) {
            owner.sendMessage("§a✓ Deine Order wurde beliefert! " + canDeliver + "x von " + deliverer.getName());
        }

        // Complete order if fulfilled
        if (order.delivered >= order.requiredAmount) {
            order.closed = true;
            if (owner != null) {
                owner.sendMessage("§a✓✓ Deine Order wurde vollständig erfüllt!");
            }
        }

        return true;
    }

    public boolean cancelOrder(Player player, String orderId) {
        Order order = orders.get(orderId);
        if (order == null || order.closed) return false;
        if (!order.owner.equals(player.getUniqueId())) return false;

        // Refund remaining amount
        double remaining = (order.requiredAmount - order.delivered) * order.pricePerItem;
        plugin.getEconomy().deposit(player.getUniqueId(), remaining);
        order.closed = true;
        order.cancelled = true;
        cleanupClosedOrder(orderId);

        player.sendMessage("§a✓ Order storniert. Rückerstattung: §e" + NumberFormatter.formatMoney(remaining));
        return true;
    }

    public List<Order> getOrders() {
        return orders.values().stream()
            .filter(order -> !order.closed)
            .sorted(Comparator.comparingLong((Order order) -> order.timestamp).reversed())
            .toList();
    }

    public List<Order> getPlayerOrders(UUID player) {
        return orders.values().stream()
            .filter(order -> order.owner.equals(player))
            .filter(order -> !order.closed || hasPendingItems(order.id))
            .sorted(Comparator.comparingLong((Order order) -> order.timestamp).reversed())
            .toList();
    }

    public Order getOrder(String orderId) {
        return orders.get(orderId);
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

    public static class BrowseSession {
        public int page;
        public String searchQuery;
        public int itemSelectPage;
        public String itemSelectSearch;
        public int collectPage;

        public BrowseSession() {
            this.page = 0;
            this.searchQuery = null;
            this.itemSelectPage = 0;
            this.itemSelectSearch = null;
            this.collectPage = 0;
        }
    }

    public CreateSession getCreateSession(UUID player) {
        return createSessions.get(player);
    }

    public void startCreateSession(UUID player) {
        createSessions.put(player, new CreateSession());
    }

    public void endCreateSession(UUID player) {
        createSessions.remove(player);
    }

    public BrowseSession getBrowseSession(UUID player) {
        return browseSessions.computeIfAbsent(player, ignored -> new BrowseSession());
    }

    public void setSearchQuery(UUID player, String query) {
        BrowseSession session = getBrowseSession(player);
        session.searchQuery = (query == null || query.isBlank()) ? null : query.trim();
        session.page = 0;
    }

    public void clearSearchQuery(UUID player) {
        BrowseSession session = getBrowseSession(player);
        session.searchQuery = null;
        session.page = 0;
    }

    // ==================== DELIVERY SESSION ====================

    public static class DeliverySession {
        public String orderId;
        public List<ItemStack> matchingItems = new ArrayList<>();
        public int matchingCount = 0;
        public enum State { DEPOSITING, CONFIRMING, DONE }
        public State state = State.DEPOSITING;

        public DeliverySession(String orderId) {
            this.orderId = orderId;
        }
    }

    public void startDeliverySession(UUID player, String orderId) {
        deliverySessions.put(player, new DeliverySession(orderId));
    }

    public DeliverySession getDeliverySession(UUID player) {
        return deliverySessions.get(player);
    }

    public void endDeliverySession(UUID player) {
        deliverySessions.remove(player);
    }

    // ==================== PENDING COLLECTIONS ====================

    public void addPendingItems(String orderId, List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        pendingCollections.computeIfAbsent(orderId, ignored -> new ArrayList<>()).addAll(items);
    }

    public List<ItemStack> getPendingItems(String orderId) {
        return pendingCollections.getOrDefault(orderId, new ArrayList<>());
    }

    public void clearPendingItems(String orderId) {
        pendingCollections.remove(orderId);
    }

    public int getPendingItemCount(String orderId) {
        List<ItemStack> items = pendingCollections.get(orderId);
        if (items == null) return 0;
        return items.stream().mapToInt(ItemStack::getAmount).sum();
    }

    public boolean hasPendingItems(String orderId) {
        return getPendingItemCount(orderId) > 0;
    }

    public int collectOrderItems(Player player, String orderId) {
        Order order = orders.get(orderId);
        if (order == null || !order.owner.equals(player.getUniqueId())) {
            return 0;
        }

        List<ItemStack> pending = pendingCollections.get(orderId);
        if (pending == null || pending.isEmpty()) {
            return 0;
        }

        int amount = 0;
        for (ItemStack item : pending) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            amount += item.getAmount();
            for (ItemStack leftover : player.getInventory().addItem(item.clone()).values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }

        clearPendingItems(orderId);
        cleanupClosedOrder(orderId);
        return amount;
    }

    private void cleanupClosedOrder(String orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            return;
        }
        if (order.closed && !hasPendingItems(orderId)) {
            orders.remove(orderId);
        }
    }

    // ==================== GUI CREATION ====================

    public Inventory createBrowseGUI(int page) {
        return createBrowseGUI(page, null);
    }

    public Inventory createBrowseGUI(int page, UUID player) {
        BrowseSession session = player == null ? new BrowseSession() : getBrowseSession(player);
        List<Order> all = getOrders();
        if (session.searchQuery != null && !session.searchQuery.isBlank()) {
            String query = session.searchQuery.toLowerCase(Locale.ROOT);
            all = all.stream()
                .filter(order -> order.itemType != null)
                .filter(order -> {
                    String materialName = order.itemType.getType().name().toLowerCase(Locale.ROOT);
                    String formattedName = formatMaterialName(order.itemType.getType()).toLowerCase(Locale.ROOT);
                    return materialName.contains(query) || formattedName.contains(query);
                })
                .toList();
        }
        int totalPages = Math.max(1, (all.size() + 27) / 28);
        page = Math.max(0, Math.min(page, totalPages - 1));
        session.page = page;

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
        navLore.add("§7Seiten: §f" + totalPages);
        if (session.searchQuery != null && !session.searchQuery.isBlank()) {
            navLore.add("§7Suche: §f\"" + session.searchQuery + "\"");
        }
        navLore.add("§8");
        if (page > 0) navLore.add("§a« Vorherige Seite (Pfeil links)");
        if (end < all.size()) navLore.add("§a» Nächste Seite (Pfeil rechts)");
        navMeta.setLore(navLore);
        navInfo.setItemMeta(navMeta);
        inv.setItem(49, navInfo);

        if (page > 0) {
            ItemStack prevBtn = mark(new ItemStack(Material.ARROW), "prev", null);
            ItemMeta prevMeta = prevBtn.getItemMeta();
            prevMeta.setDisplayName("§e§l◄ " + toSmallCaps("VORHERIGE SEITE"));
            prevBtn.setItemMeta(prevMeta);
            inv.setItem(45, prevBtn);
        }

        ItemStack searchBtn = mark(new ItemStack(Material.COMPASS), "search", null);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        searchMeta.setDisplayName("§e§l🔍 " + toSmallCaps("SUCHEN"));
        List<String> searchLore = new ArrayList<>();
        searchLore.add("§8");
        if (session.searchQuery != null && !session.searchQuery.isBlank()) {
            searchLore.add("§7Aktive Suche:");
            searchLore.add("§f\"" + session.searchQuery + "\"");
            searchLore.add("§8");
            searchLore.add("§aLinksklick §8- §7Neue Sign-Suche");
            searchLore.add("§cRechtsklick §8- §7Suche löschen");
        } else {
            searchLore.add("§7Suche nach Item-Namen");
            searchLore.add("§8");
            searchLore.add("§e▸ Öffnet die Sign-Eingabe");
        }
        searchMeta.setLore(searchLore);
        searchBtn.setItemMeta(searchMeta);
        inv.setItem(48, searchBtn);

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

        if (page < totalPages - 1) {
            ItemStack nextBtn = mark(new ItemStack(Material.ARROW), "next", null);
            ItemMeta nextMeta = nextBtn.getItemMeta();
            nextMeta.setDisplayName("§e§l" + toSmallCaps("NÄCHSTE SEITE") + " ►");
            nextBtn.setItemMeta(nextMeta);
            inv.setItem(53, nextBtn);
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

        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53}) {
            inv.setItem(i, border);
        }

        // Collect button (slot 49)
        int totalPending = orders.values().stream()
            .filter(o -> o.owner.equals(player))
            .filter(o -> hasPendingItems(o.id))
            .mapToInt(o -> getPendingItemCount(o.id)).sum();
        if (totalPending > 0) {
            ItemStack collectBtn = mark(new ItemStack(Material.CHEST), "open_collect", null);
            ItemMeta collectMeta = collectBtn.getItemMeta();
            collectMeta.setDisplayName("§a§l✓ " + toSmallCaps("ITEMS ABHOLEN") + " §7(" + totalPending + "x)");
            List<String> collectLore = new ArrayList<>();
            collectLore.add("§8");
            collectLore.add("§7Du hast §a" + totalPending + "x §7Items");
            collectLore.add("§7zum Abholen bereit.");
            collectLore.add("§8");
            collectLore.add("§a▸ Klicken zum Öffnen");
            collectMeta.setLore(collectLore);
            collectBtn.setItemMeta(collectMeta);
            inv.setItem(49, collectBtn);
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
            lore.add("§7Abholbar: " + (getPendingItemCount(order.id) > 0 ? "§a" + getPendingItemCount(order.id) + "x" : "§c0x"));
            lore.add("§8");
            lore.add("§6⛃ §7Preis/Stück: §e" + NumberFormatter.formatMoney(order.pricePerItem));
            lore.add("§6⛃ §7Gesamt: §e" + NumberFormatter.formatMoney(order.requiredAmount * order.pricePerItem));
            lore.add("§8§m                    ");
            lore.add("§8");
            lore.add("§e§l▸ " + toSmallCaps("OPTIONEN ÖFFNEN"));
            lore.add("§7Stornieren, Infos oder Abholen");
            displayMeta.setLore(lore);
            display.setItemMeta(displayMeta);
            mark(display, "my_order_open", order.id);

            inv.setItem(slot, display);
            slot++;
        }

        return inv;
    }

    public Inventory createOrderDetailGUI(UUID player, String orderId) {
        Order order = orders.get(orderId);
        if (order == null || !order.owner.equals(player)) {
            return createMyOrdersGUI(player);
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§9§l" + toSmallCaps("ORDER DETAILS"));
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("§8⬛");
        border.setItemMeta(borderMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, border);
        }

        ItemStack cancelItem = mark(new ItemStack(order.closed ? Material.GRAY_DYE : Material.RED_STAINED_GLASS_PANE), order.closed ? "disabled" : "order_cancel_prompt", order.id);
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(order.closed ? "§7§lNICHT STORNIERBAR" : "§c§lORDER STORNIEREN");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("§8────────────────");
        if (order.closed) {
            cancelLore.add(order.cancelled ? "§7Diese Order wurde bereits storniert." : "§7Diese Order ist bereits abgeschlossen.");
        } else {
            cancelLore.add("§7Offener Rest wird erstattet.");
            cancelLore.add("§7Stornierung braucht Bestätigung.");
            cancelLore.add("§8");
            cancelLore.add("§c▸ Klicken zum Fortfahren");
        }
        cancelMeta.setLore(cancelLore);
        cancelItem.setItemMeta(cancelMeta);
        inv.setItem(11, cancelItem);

        ItemStack info = order.itemType.clone();
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§l" + formatMaterialName(order.itemType.getType()));
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§8────────────────");
            infoLore.add("§7Status: " + (order.cancelled ? "§cStorniert" : order.closed ? "§aAbgeschlossen" : "§eAktiv"));
            infoLore.add("§7Benötigt: §f" + order.requiredAmount + "x");
            infoLore.add("§7Geliefert: §a" + order.delivered + "x");
            infoLore.add("§7Verbleibend: §e" + Math.max(0, order.requiredAmount - order.delivered) + "x");
            infoLore.add("§7Abholbar: " + (getPendingItemCount(order.id) > 0 ? "§a" + getPendingItemCount(order.id) + "x" : "§c0x"));
            infoLore.add("§8");
            infoLore.add("§6⛃ §7Preis/Stück: §e" + NumberFormatter.formatMoney(order.pricePerItem));
            infoLore.add("§6⛃ §7Gesamt: §e" + NumberFormatter.formatMoney(order.requiredAmount * order.pricePerItem));
            infoLore.add("§8────────────────");
            infoMeta.setLore(infoLore);
            infoMeta.getPersistentDataContainer().set(UI_KEY, PersistentDataType.STRING, "true");
            infoMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "disabled");
            info.setItemMeta(infoMeta);
        }
        inv.setItem(13, info);

        ItemStack collectItem = mark(new ItemStack(getPendingItemCount(order.id) > 0 ? Material.CHEST : Material.HOPPER), getPendingItemCount(order.id) > 0 ? "order_collect" : "disabled", order.id);
        ItemMeta collectMeta = collectItem.getItemMeta();
        collectMeta.setDisplayName(getPendingItemCount(order.id) > 0 ? "§a§lITEMS ABHOLEN" : "§7§lNICHTS ABZUHOLEN");
        List<String> collectLore = new ArrayList<>();
        collectLore.add("§8────────────────");
        collectLore.add("§7Abholbare Items: " + (getPendingItemCount(order.id) > 0 ? "§a" + getPendingItemCount(order.id) + "x" : "§c0x"));
        if (getPendingItemCount(order.id) > 0) {
            collectLore.add("§8");
            collectLore.add("§a▸ Klicken zum Einsammeln");
        }
        collectMeta.setLore(collectLore);
        collectItem.setItemMeta(collectMeta);
        inv.setItem(15, collectItem);

        ItemStack back = mark(new ItemStack(Material.ARROW), "order_detail_back", null);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§e§lZURÜCK");
        back.setItemMeta(backMeta);
        inv.setItem(22, back);
        return inv;
    }

    public Inventory createCollectGUI(UUID player) {
        return createCollectGUI(player, 0);
    }

    public Inventory createCollectGUI(UUID player, int page) {
        // Collect all pending orders
        List<Order> withPending = orders.values().stream()
            .filter(o -> o.owner.equals(player))
            .filter(o -> hasPendingItems(o.id))
            .sorted(Comparator.comparingLong((Order o) -> o.timestamp).reversed())
            .toList();

        int totalPending = withPending.stream().mapToInt(o -> getPendingItemCount(o.id)).sum();
        int totalPages = Math.max(1, (withPending.size() + 27) / 28);
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, "§a§l" + toSmallCaps("ITEMS ABHOLEN") + " §8(" + (page + 1) + "/" + totalPages + ")");

        // Fill borders
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("§8⬛");
        border.setItemMeta(borderMeta);
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53}) {
            inv.setItem(i, border);
        }

        // Info header
        ItemStack info = new ItemStack(Material.CHEST);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§a§lAbholbare Items");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8────────────────");
        infoLore.add("§7Orders mit Items: §f" + withPending.size());
        infoLore.add("§7Gesamt abholbar: §a" + totalPending + "x");
        infoLore.add("§7Seite: §f" + (page + 1) + "§7/§f" + totalPages);
        infoLore.add("§8────────────────");
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        // Show orders with pending items (paginated)
        int start = page * 28;
        int end = Math.min(start + 28, withPending.size());
        int slot = 10;
        for (int i = start; i < end; i++) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            if (slot >= 44) break;

            Order order = withPending.get(i);
            int pending = getPendingItemCount(order.id);
            ItemStack display = order.itemType.clone();
            ItemMeta displayMeta = display.getItemMeta();
            List<String> lore = displayMeta.hasLore() ? new ArrayList<>(displayMeta.getLore()) : new ArrayList<>();
            lore.add("§8");
            lore.add("§8§m                    ");
            lore.add("§7Benötigt: §f" + order.requiredAmount + "x");
            lore.add("§7Geliefert: §a" + order.delivered + "x");
            lore.add("§7Abholbar: §a" + pending + "x");
            lore.add("§8§m                    ");
            lore.add("§8");
            lore.add("§a§l▸ Klicken zum Abholen");
            displayMeta.setLore(lore);
            display.setItemMeta(displayMeta);
            mark(display, "collect_single", order.id);
            inv.setItem(slot, display);
            slot++;
        }

        if (withPending.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = empty.getItemMeta();
            emptyMeta.setDisplayName("§7Keine Items abholbar");
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("§8");
            emptyLore.add("§7Es gibt aktuell nichts abzuholen.");
            emptyMeta.setLore(emptyLore);
            empty.setItemMeta(emptyMeta);
            inv.setItem(22, empty);
        }

        // Navigation - prev page
        if (page > 0) {
            ItemStack prevBtn = mark(new ItemStack(Material.ARROW), "collect_prev", null);
            ItemMeta prevMeta = prevBtn.getItemMeta();
            prevMeta.setDisplayName("§e§l◄ " + toSmallCaps("VORHERIGE SEITE"));
            prevBtn.setItemMeta(prevMeta);
            inv.setItem(45, prevBtn);
        } else {
            // Back button
            ItemStack back = mark(new ItemStack(Material.ARROW), "collect_back", null);
            ItemMeta backBtnMeta = back.getItemMeta();
            backBtnMeta.setDisplayName("§e§lZURÜCK");
            back.setItemMeta(backBtnMeta);
            inv.setItem(45, back);
        }

        // Collect All button
        if (totalPending > 0) {
            ItemStack collectAll = mark(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "collect_all", null);
            ItemMeta collectAllMeta = collectAll.getItemMeta();
            collectAllMeta.setDisplayName("§a§l✓ " + toSmallCaps("ALLES ABHOLEN"));
            List<String> collectAllLore = new ArrayList<>();
            collectAllLore.add("§8────────────────");
            collectAllLore.add("§7Alle §a" + totalPending + "x §7Items");
            collectAllLore.add("§7auf einmal abholen.");
            collectAllLore.add("§8────────────────");
            collectAllLore.add("§a▸ Klicken zum Einsammeln");
            collectAllMeta.setLore(collectAllLore);
            collectAll.setItemMeta(collectAllMeta);
            inv.setItem(48, collectAll);
        }

        // Drop All button
        if (totalPending > 0) {
            ItemStack dropAll = mark(new ItemStack(Material.DROPPER), "collect_drop_all", null);
            ItemMeta dropMeta = dropAll.getItemMeta();
            dropMeta.setDisplayName("§e§l↓ " + toSmallCaps("ALLES DROPPEN"));
            List<String> dropLore = new ArrayList<>();
            dropLore.add("§8────────────────");
            dropLore.add("§7Alle §a" + totalPending + "x §7Items");
            dropLore.add("§7auf den Boden werfen.");
            dropLore.add("§8────────────────");
            dropLore.add("§e▸ Klicken zum Droppen");
            dropMeta.setLore(dropLore);
            dropAll.setItemMeta(dropMeta);
            inv.setItem(50, dropAll);
        }

        // Page info
        ItemStack pageInfo = mark(new ItemStack(Material.PAPER), "disabled", null);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        pageMeta.setDisplayName("§e§l📄 " + toSmallCaps("SEITE") + " §f" + (page + 1) + "§7/§f" + totalPages);
        pageInfo.setItemMeta(pageMeta);
        inv.setItem(49, pageInfo);

        // Navigation - next page
        if (page < totalPages - 1) {
            ItemStack nextBtn = mark(new ItemStack(Material.ARROW), "collect_next", null);
            ItemMeta nextMeta = nextBtn.getItemMeta();
            nextMeta.setDisplayName("§e§l" + toSmallCaps("NÄCHSTE SEITE") + " ►");
            nextBtn.setItemMeta(nextMeta);
            inv.setItem(53, nextBtn);
        }

        return inv;
    }

    public int collectAllOrderItems(Player player) {
        UUID uuid = player.getUniqueId();
        List<Order> withPending = orders.values().stream()
            .filter(o -> o.owner.equals(uuid))
            .filter(o -> hasPendingItems(o.id))
            .toList();

        int total = 0;
        for (Order order : withPending) {
            total += collectOrderItems(player, order.id);
        }
        return total;
    }

    public int dropAllOrderItems(Player player) {
        UUID uuid = player.getUniqueId();
        List<Order> withPending = orders.values().stream()
            .filter(o -> o.owner.equals(uuid))
            .filter(o -> hasPendingItems(o.id))
            .toList();

        int total = 0;
        for (Order order : withPending) {
            List<ItemStack> pending = pendingCollections.get(order.id);
            if (pending == null || pending.isEmpty()) continue;
            for (ItemStack item : pending) {
                if (item == null || item.getType() == Material.AIR) continue;
                total += item.getAmount();
                player.getWorld().dropItemNaturally(player.getLocation(), item.clone());
            }
            clearPendingItems(order.id);
            cleanupClosedOrder(order.id);
        }
        return total;
    }

    public Inventory createOrderCancelConfirmGUI(UUID player, String orderId) {
        Order order = orders.get(orderId);
        if (order == null || !order.owner.equals(player)) {
            return createMyOrdersGUI(player);
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§c§l" + toSmallCaps("ORDER STORNIEREN"));
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("§8⬛");
        border.setItemMeta(borderMeta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, border);
        }

        double refund = Math.max(0, (order.requiredAmount - order.delivered) * order.pricePerItem);

        ItemStack deny = mark(new ItemStack(Material.RED_STAINED_GLASS_PANE), "order_detail_back", order.id);
        ItemMeta denyMeta = deny.getItemMeta();
        denyMeta.setDisplayName("§7§lABBRECHEN");
        deny.setItemMeta(denyMeta);
        inv.setItem(11, deny);

        ItemStack info = order.itemType.clone();
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§c§lStornierung bestätigen");
            List<String> infoLore = new ArrayList<>();
            infoLore.add("§8────────────────");
            infoLore.add("§7Item: §f" + formatMaterialName(order.itemType.getType()));
            infoLore.add("§7Verbleibend: §e" + Math.max(0, order.requiredAmount - order.delivered) + "x");
            infoLore.add("§6⛃ §7Erstattung: §e" + NumberFormatter.formatMoney(refund));
            infoLore.add("§8────────────────");
            infoMeta.setLore(infoLore);
            infoMeta.getPersistentDataContainer().set(UI_KEY, PersistentDataType.STRING, "true");
            infoMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "disabled");
            info.setItemMeta(infoMeta);
        }
        inv.setItem(13, info);

        ItemStack confirm = mark(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "order_cancel_confirm", order.id);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§c§lJA, STORNIEREN");
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("§8────────────────");
        confirmLore.add("§7Die Order wird geschlossen.");
        confirmLore.add("§7Offenes Geld wird erstattet.");
        confirmLore.add("§8");
        confirmLore.add("§c▸ Klicken zum Bestätigen");
        confirmMeta.setLore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        inv.setItem(15, confirm);
        return inv;
    }

    public Inventory createNewOrderGUI(UUID player) {
        CreateSession session = createSessions.get(player);
        if (session == null) {
            session = new CreateSession();
            createSessions.put(player, session);
        }

        Inventory inv = Bukkit.createInventory(null, 9, "§a§l" + toSmallCaps("NEUE ORDER"));

        // Slot 4: Display the selected item (read-only)
        if (session.item != null) {
            ItemStack displayItem = session.item.clone();
            ItemMeta diMeta = displayItem.getItemMeta();
            if (diMeta != null) {
                String itemName = formatMaterialName(session.item.getType());
                diMeta.setDisplayName("§e§l" + itemName);
                List<String> diLore = new ArrayList<>();
                diLore.add("§8");
                diLore.add("§7Ausgewähltes Item");
                diLore.add("§8");
                diMeta.setLore(diLore);
                diMeta.getPersistentDataContainer().set(UI_KEY, PersistentDataType.STRING, "true");
                diMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "disabled");
                displayItem.setItemMeta(diMeta);
            }
            inv.setItem(4, displayItem);
        }

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
                priceLore.add("§7Aktueller Preis: §e" + NumberFormatter.formatMoney(session.price) + "/Stück");
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
        }

        return inv;
    }

    // ==================== ITEM SELECT GUI ====================

    public Inventory createItemSelectGUI(UUID player, int page) {
        BrowseSession session = getBrowseSession(player);
        List<Map.Entry<Material, Double>> items = getSelectableItems(session.itemSelectSearch);

        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        session.itemSelectPage = page;

        Inventory inv = Bukkit.createInventory(null, 54, "§a§l" + toSmallCaps("ITEM WAHLEN") + " §8(" + (page + 1) + ")");

        // Fill borders
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta bMeta = border.getItemMeta();
        bMeta.setDisplayName("§8⬛");
        border.setItemMeta(bMeta);

        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,51,52}) {
            inv.setItem(i, border);
        }

        // Items (28 slots)
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());

        int slot = 10;
        for (int i = start; i < end; i++) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;

            Map.Entry<Material, Double> entry = items.get(i);
            Material mat = entry.getKey();
            double price = entry.getValue();

            ItemStack display = new ItemStack(mat);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                String itemName = formatMaterialName(mat);
                meta.setDisplayName("§e§l" + itemName);
                List<String> lore = new ArrayList<>();
                lore.add("§8");
                lore.add("§7Wert: §a$" + String.format("%.0f", price));
                lore.add("§8");
                lore.add("§a▸ Klicken zum Auswählen");
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(UI_KEY, PersistentDataType.STRING, "true");
                meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "select_item");
                meta.getPersistentDataContainer().set(MATERIAL_KEY, PersistentDataType.STRING, mat.name());
                display.setItemMeta(meta);
            }
            inv.setItem(slot, display);
            slot++;
        }

        // Navigation - Prev
        if (page > 0) {
            ItemStack prevBtn = mark(new ItemStack(Material.ARROW), "item_select_prev", null);
            ItemMeta prevMeta = prevBtn.getItemMeta();
            prevMeta.setDisplayName("§e§l◄ " + toSmallCaps("VORHERIGE SEITE"));
            prevBtn.setItemMeta(prevMeta);
            inv.setItem(45, prevBtn);
        }

        // Search
        ItemStack searchBtn = mark(new ItemStack(Material.COMPASS), "item_select_search", null);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        searchMeta.setDisplayName("§e§l🔍 " + toSmallCaps("SUCHEN"));
        List<String> searchLore = new ArrayList<>();
        searchLore.add("§8");
        if (session.itemSelectSearch != null && !session.itemSelectSearch.isBlank()) {
            searchLore.add("§7Aktive Suche:");
            searchLore.add("§f\"" + session.itemSelectSearch + "\"");
            searchLore.add("§8");
            searchLore.add("§aLinksklick §8- §7Neue Sign-Suche");
            searchLore.add("§cRechtsklick §8- §7Suche löschen");
        } else {
            searchLore.add("§7Suche nach Item-Namen");
            searchLore.add("§8");
            searchLore.add("§e▸ Öffnet die Sign-Eingabe");
        }
        searchMeta.setLore(searchLore);
        searchBtn.setItemMeta(searchMeta);
        inv.setItem(48, searchBtn);

        // Page info
        ItemStack navInfo = mark(new ItemStack(Material.PAPER), "disabled", null);
        ItemMeta navMeta = navInfo.getItemMeta();
        navMeta.setDisplayName("§e§l📄 " + toSmallCaps("SEITE") + " §f" + (page + 1) + "/" + totalPages);
        List<String> navLore = new ArrayList<>();
        navLore.add("§8");
        navLore.add("§7Items: §f" + items.size());
        if (session.itemSelectSearch != null && !session.itemSelectSearch.isBlank()) {
            navLore.add("§7Suche: §f\"" + session.itemSelectSearch + "\"");
        }
        navMeta.setLore(navLore);
        navInfo.setItemMeta(navMeta);
        inv.setItem(49, navInfo);

        // Back button
        ItemStack backBtn = mark(new ItemStack(Material.RED_STAINED_GLASS_PANE), "item_select_back", null);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName("§c§l« " + toSmallCaps("ZURUCK"));
        backBtn.setItemMeta(backMeta);
        inv.setItem(50, backBtn);

        // Navigation - Next
        if (page < totalPages - 1) {
            ItemStack nextBtn = mark(new ItemStack(Material.ARROW), "item_select_next", null);
            ItemMeta nextMeta = nextBtn.getItemMeta();
            nextMeta.setDisplayName("§e§l" + toSmallCaps("NÄCHSTE SEITE") + " ►");
            nextBtn.setItemMeta(nextMeta);
            inv.setItem(53, nextBtn);
        }

        return inv;
    }

    // ==================== DELIVERY GUI ====================

    public Inventory createDeliveryChestGUI(Player player, String orderId) {
        Order order = orders.get(orderId);
        if (order == null) return null;

        startDeliverySession(player.getUniqueId(), orderId);

        int needed = order.requiredAmount - order.delivered;

        Inventory inv = Bukkit.createInventory(null, 54, "§2§l" + toSmallCaps("BELIEFERUNG"));

        // Top row - info bar
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta bMeta = border.getItemMeta();
        bMeta.setDisplayName("§8⬛");
        bMeta.getPersistentDataContainer().set(UI_KEY, PersistentDataType.STRING, "true");
        border.setItemMeta(bMeta);

        for (int i : new int[]{0,1,2,3,5,6,7,8}) {
            inv.setItem(i, border);
        }

        // Info item (slot 4)
        ItemStack info = order.itemType.clone();
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§e§l" + formatMaterialName(order.itemType.getType()));
        List<String> lore = new ArrayList<>();
        lore.add("§8");
        lore.add("§7Benötigt: §f" + needed + "x");
        lore.add("§6⛃ §7Preis/Stück: §e" + NumberFormatter.formatMoney(order.pricePerItem));
        lore.add("§8");
        lore.add("§7Lege Items in das Inventar");
        lore.add("§7Shulker werden geöffnet");
        lore.add("§8");
        infoMeta.setLore(lore);
        infoMeta.getPersistentDataContainer().set(UI_KEY, PersistentDataType.STRING, "true");
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        return inv;
    }

    public Inventory createDeliveryConfirmGUI(Player player, int itemCount, double totalPayment) {
        Inventory inv = Bukkit.createInventory(null, 27, "§2§l" + toSmallCaps("LIEFERUNG BESTATIGEN"));

        // Fill all with border
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta bMeta = border.getItemMeta();
        bMeta.setDisplayName("§8⬛");
        border.setItemMeta(bMeta);
        for (int i = 0; i < 27; i++) inv.setItem(i, border);

        // Confirm (slot 11)
        ItemStack confirm = mark(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "delivery_confirm", null);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§l✓ " + toSmallCaps("BESTATIGEN"));
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("§8");
        confirmLore.add("§7Menge: §f" + itemCount + "x");
        confirmLore.add("§6⛃ §7Verdienst: §a§l+" + NumberFormatter.formatMoney(totalPayment));
        confirmLore.add("§8");
        confirmLore.add("§a▸ Klicken zum Bestätigen");
        confirmMeta.setLore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        inv.setItem(11, confirm);

        // Info (slot 13)
        DeliverySession session = getDeliverySession(player.getUniqueId());
        if (session != null) {
            Order order = orders.get(session.orderId);
            if (order != null) {
                ItemStack info = order.itemType.clone();
                ItemMeta infoMeta = info.getItemMeta();
                infoMeta.setDisplayName("§e§l" + formatMaterialName(order.itemType.getType()));
                List<String> infoLore = new ArrayList<>();
                infoLore.add("§8");
                infoLore.add("§7Liefermenge: §f" + itemCount + "x");
                infoLore.add("§6⛃ §7Gesamt: §a" + NumberFormatter.formatMoney(totalPayment));
                infoLore.add("§8");
                infoMeta.setLore(infoLore);
                infoMeta.getPersistentDataContainer().set(UI_KEY, PersistentDataType.STRING, "true");
                info.setItemMeta(infoMeta);
                inv.setItem(13, info);
            }
        }

        // Cancel (slot 15)
        ItemStack cancel = mark(new ItemStack(Material.RED_STAINED_GLASS_PANE), "delivery_cancel", null);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§l✖ " + toSmallCaps("ABBRECHEN"));
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("§8");
        cancelLore.add("§7Items werden zurückgegeben");
        cancelLore.add("§8");
        cancelLore.add("§c▸ Klicken zum Abbrechen");
        cancelMeta.setLore(cancelLore);
        cancel.setItemMeta(cancelMeta);
        inv.setItem(15, cancel);

        return inv;
    }

    private List<Map.Entry<Material, Double>> getSelectableItems(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return plugin.getWorthManager().getBaseValues().entrySet().stream()
            .filter(e -> e.getValue() > 0 && e.getKey().isItem())
            .filter(e -> !isUnobtainable(e.getKey()))
            .filter(e -> normalizedQuery.isEmpty()
                || formatMaterialName(e.getKey()).toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || e.getKey().name().toLowerCase(Locale.ROOT).contains(normalizedQuery))
            .sorted(Map.Entry.<Material, Double>comparingByValue().reversed())
            .collect(Collectors.toList());
    }

    private static boolean isUnobtainable(Material mat) {
        String name = mat.name();
        return UNOBTAINABLE.contains(name)
            || name.contains("SPAWN_EGG")
            || name.contains("COMMAND_BLOCK")
            || name.startsWith("INFESTED_")
            || name.endsWith("_WALL_HEAD")
            || name.endsWith("_WALL_SKULL")
            || name.endsWith("_WALL_BANNER")
            || name.endsWith("_WALL_SIGN")
            || name.endsWith("_WALL_HANGING_SIGN")
            || name.endsWith("_WALL_TORCH")
            || name.endsWith("_WALL_FAN");
    }

    public String getMaterial(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(MATERIAL_KEY, PersistentDataType.STRING);
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
            title.contains("ᴏʀᴅᴇʀ ᴅᴇᴛᴀɪʟs") ||
            title.contains("ᴏʀᴅᴇʀ sᴛᴏʀɴɪᴇʀᴇɴ") ||
            title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ") ||
            title.contains("ɪᴛᴇᴍ ᴡᴀʜʟᴇɴ") ||
            title.contains("ʟɪᴇғᴇʀᴜɴɢ ʙᴇsᴛᴀᴛɪɢᴇɴ") ||
            title.contains("ɪᴛᴇᴍs ᴀʙʜᴏʟᴇɴ")
        );
    }

    public boolean isDeliveryChestGUI(String title) {
        return title != null && title.contains("ʙᴇʟɪᴇғᴇʀᴜɴɢ");
    }

    public boolean isDeliveryConfirmGUI(String title) {
        return title != null && title.contains("ʟɪᴇғᴇʀᴜɴɢ ʙᴇsᴛᴀᴛɪɢᴇɴ");
    }

    public String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
    }

    public String getOrderId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ORDER_ID_KEY, PersistentDataType.STRING);
    }

    private static final java.util.Map<String, String> DISPLAY_NAMES = java.util.Map.ofEntries(
        java.util.Map.entry("TURTLE_HELMET", "Turtle Shell"),
        java.util.Map.entry("LEATHER_CHESTPLATE", "Leather Tunic"),
        java.util.Map.entry("LEATHER_HELMET", "Leather Cap"),
        java.util.Map.entry("LEATHER_LEGGINGS", "Leather Pants"),
        java.util.Map.entry("COD", "Raw Cod"),
        java.util.Map.entry("SALMON", "Raw Salmon"),
        java.util.Map.entry("CHICKEN", "Raw Chicken"),
        java.util.Map.entry("BEEF", "Raw Beef"),
        java.util.Map.entry("MUTTON", "Raw Mutton"),
        java.util.Map.entry("PORKCHOP", "Raw Porkchop"),
        java.util.Map.entry("RABBIT", "Raw Rabbit"),
        java.util.Map.entry("COOKED_BEEF", "Steak"),
        java.util.Map.entry("REDSTONE", "Redstone Dust"),
        java.util.Map.entry("REPEATER", "Redstone Repeater"),
        java.util.Map.entry("COMPARATOR", "Redstone Comparator"),
        java.util.Map.entry("SLIME_BALL", "Slimeball"),
        java.util.Map.entry("VINE", "Vines"),
        java.util.Map.entry("HAY_BLOCK", "Hay Bale"),
        java.util.Map.entry("JACK_O_LANTERN", "Jack o'Lantern"),
        java.util.Map.entry("POTION", "Potion"),
        java.util.Map.entry("SPLASH_POTION", "Splash Potion"),
        java.util.Map.entry("LINGERING_POTION", "Lingering Potion"),
        java.util.Map.entry("COD_BUCKET", "Bucket of Cod"),
        java.util.Map.entry("SALMON_BUCKET", "Bucket of Salmon"),
        java.util.Map.entry("PUFFERFISH_BUCKET", "Bucket of Pufferfish"),
        java.util.Map.entry("TROPICAL_FISH_BUCKET", "Bucket of Tropical Fish"),
        java.util.Map.entry("AXOLOTL_BUCKET", "Bucket of Axolotl"),
        java.util.Map.entry("TADPOLE_BUCKET", "Bucket of Tadpole"),
        java.util.Map.entry("DIAMOND_BLOCK", "Block of Diamond"),
        java.util.Map.entry("NETHERITE_BLOCK", "Block of Netherite"),
        java.util.Map.entry("EMERALD_BLOCK", "Block of Emerald"),
        java.util.Map.entry("GOLD_BLOCK", "Block of Gold"),
        java.util.Map.entry("IRON_BLOCK", "Block of Iron"),
        java.util.Map.entry("COAL_BLOCK", "Block of Coal"),
        java.util.Map.entry("COPPER_BLOCK", "Block of Copper"),
        java.util.Map.entry("LAPIS_BLOCK", "Block of Lapis Lazuli"),
        java.util.Map.entry("REDSTONE_BLOCK", "Block of Redstone"),
        java.util.Map.entry("AMETHYST_BLOCK", "Block of Amethyst"),
        java.util.Map.entry("BAMBOO_BLOCK", "Block of Bamboo"),
        java.util.Map.entry("RESIN_BLOCK", "Block of Resin"),
        java.util.Map.entry("QUARTZ_BLOCK", "Block of Quartz"),
        java.util.Map.entry("RAW_COPPER_BLOCK", "Block of Raw Copper"),
        java.util.Map.entry("RAW_GOLD_BLOCK", "Block of Raw Gold"),
        java.util.Map.entry("RAW_IRON_BLOCK", "Block of Raw Iron"),
        java.util.Map.entry("WAXED_COPPER_BLOCK", "Waxed Block of Copper"),
        java.util.Map.entry("STRIPPED_BAMBOO_BLOCK", "Block of Stripped Bamboo"),
        java.util.Map.entry("QUARTZ", "Nether Quartz"),
        java.util.Map.entry("SMOOTH_QUARTZ", "Smooth Quartz Block"),
        java.util.Map.entry("LAPIS_ORE", "Lapis Lazuli Ore"),
        java.util.Map.entry("DEEPSLATE_LAPIS_ORE", "Deepslate Lapis Lazuli Ore"),
        java.util.Map.entry("COAST_ARMOR_TRIM_SMITHING_TEMPLATE", "Coast Armor Trim"),
        java.util.Map.entry("DUNE_ARMOR_TRIM_SMITHING_TEMPLATE", "Dune Armor Trim"),
        java.util.Map.entry("EYE_ARMOR_TRIM_SMITHING_TEMPLATE", "Eye Armor Trim"),
        java.util.Map.entry("HOST_ARMOR_TRIM_SMITHING_TEMPLATE", "Host Armor Trim"),
        java.util.Map.entry("RAISER_ARMOR_TRIM_SMITHING_TEMPLATE", "Raiser Armor Trim"),
        java.util.Map.entry("RIB_ARMOR_TRIM_SMITHING_TEMPLATE", "Rib Armor Trim"),
        java.util.Map.entry("SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE", "Sentry Armor Trim"),
        java.util.Map.entry("SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE", "Shaper Armor Trim"),
        java.util.Map.entry("SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE", "Silence Armor Trim"),
        java.util.Map.entry("SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE", "Snout Armor Trim"),
        java.util.Map.entry("SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE", "Spire Armor Trim"),
        java.util.Map.entry("TIDE_ARMOR_TRIM_SMITHING_TEMPLATE", "Tide Armor Trim"),
        java.util.Map.entry("VEX_ARMOR_TRIM_SMITHING_TEMPLATE", "Vex Armor Trim"),
        java.util.Map.entry("WARD_ARMOR_TRIM_SMITHING_TEMPLATE", "Ward Armor Trim"),
        java.util.Map.entry("WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE", "Wayfinder Armor Trim"),
        java.util.Map.entry("WILD_ARMOR_TRIM_SMITHING_TEMPLATE", "Wild Armor Trim"),
        java.util.Map.entry("NETHERITE_UPGRADE_SMITHING_TEMPLATE", "Netherite Upgrade")
    );

    private String formatMaterialName(Material material) {
        String override = DISPLAY_NAMES.get(material.name());
        if (override != null) return override;
        String[] parts = material.name().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) result.append(' ');
            result.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }
}
