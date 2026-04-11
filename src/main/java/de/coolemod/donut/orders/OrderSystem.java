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

    public boolean completeDelivery(String orderId, Player deliverer, int count) {
        Order order = orders.get(orderId);
        if (order == null) return false;

        int canDeliver = Math.min(count, order.requiredAmount - order.delivered);
        if (canDeliver <= 0) return false;

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

    public static class BrowseSession {
        public int page;
        public String searchQuery;
        public int itemSelectPage;
        public String itemSelectSearch;

        public BrowseSession() {
            this.page = 0;
            this.searchQuery = null;
            this.itemSelectPage = 0;
            this.itemSelectSearch = null;
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
            title.contains("ɴᴇᴜᴇ ᴏʀᴅᴇʀ") ||
            title.contains("ɪᴛᴇᴍ ᴡᴀʜʟᴇɴ") ||
            title.contains("ʟɪᴇғᴇʀᴜɴɢ ʙᴇsᴛᴀᴛɪɢᴇɴ")
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

    private String formatMaterialName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) result.append(' ');
            result.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }
}
