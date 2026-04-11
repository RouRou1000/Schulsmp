package de.coolemod.donut.auctionhouse;

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

/**
 * Komplett neues Auction House System
 */
public class AuctionHouse {
    private final DonutPlugin plugin;
    private final Map<String, Auction> auctions = new HashMap<>();
    private final Map<UUID, CreateSession> createSessions = new HashMap<>();
    private final Map<UUID, BrowseSession> browseSessions = new HashMap<>();

    private final NamespacedKey UI_KEY;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey AUCTION_ID_KEY;

    public AuctionHouse(DonutPlugin plugin) {
        this.plugin = plugin;
        this.UI_KEY = new NamespacedKey(plugin, "ah_ui");
        this.ACTION_KEY = new NamespacedKey(plugin, "ah_action");
        this.AUCTION_ID_KEY = new NamespacedKey(plugin, "ah_id");
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

    // ==================== AUCTION MANAGEMENT ====================

    public static class Auction {
        public String id;
        public UUID seller;
        public ItemStack item;
        public double price;
        public long timestamp;

        public Auction(String id, UUID seller, ItemStack item, double price) {
            this.id = id;
            this.seller = seller;
            this.item = item.clone();
            this.price = price;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public String createAuction(UUID seller, ItemStack item, double price) {
        String id = UUID.randomUUID().toString();
        auctions.put(id, new Auction(id, seller, item, price));
        return id;
    }

    public boolean buyAuction(Player buyer, String auctionId) {
        Auction auction = auctions.get(auctionId);
        if (auction == null) return false;

        // Check balance
        if (plugin.getEconomy().getBalance(buyer.getUniqueId()) < auction.price) {
            buyer.sendMessage("§cNicht genug Geld!");
            return false;
        }

        // Transfer money
        plugin.getEconomy().withdraw(buyer.getUniqueId(), auction.price);
        plugin.getEconomy().deposit(auction.seller, auction.price);

        // Give item
        HashMap<Integer, ItemStack> leftover = buyer.getInventory().addItem(auction.item);
        if (!leftover.isEmpty()) {
            leftover.values().forEach(item -> buyer.getWorld().dropItem(buyer.getLocation(), item));
        }

        // Remove auction
        auctions.remove(auctionId);

        // Notify
        buyer.sendMessage("§a✓ Gekauft für §e" + NumberFormatter.formatMoney(auction.price));
        Player seller = Bukkit.getPlayer(auction.seller);
        if (seller != null) {
            seller.sendMessage("§a✓ Deine Auktion wurde verkauft! +§e" + NumberFormatter.formatMoney(auction.price));
        }

        return true;
    }

    public List<Auction> getAuctions() {
        return new ArrayList<>(auctions.values());
    }

    public List<Auction> getFilteredAuctions(UUID player) {
        BrowseSession session = getBrowseSession(player);
        List<Auction> result = new ArrayList<>(auctions.values());

        // Apply search filter
        if (session.searchQuery != null && !session.searchQuery.trim().isEmpty()) {
            String query = session.searchQuery.toLowerCase().trim();
            result.removeIf(auction -> {
                if (auction == null || auction.item == null) return true;
                String itemName = auction.item.getType().name().toLowerCase().replace("_", " ");
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
                result.sort((a, b) -> Double.compare(a.price, b.price));
                break;
            case PRICE_HIGH:
                result.sort((a, b) -> Double.compare(b.price, a.price));
                break;
            case NAME:
                result.sort((a, b) -> {
                    if (a == null || a.item == null) return 1;
                    if (b == null || b.item == null) return -1;
                    return a.item.getType().name().compareTo(b.item.getType().name());
                });
                break;
        }

        return result;
    }

    public List<Auction> getPlayerAuctions(UUID player) {
        List<Auction> result = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.seller.equals(player)) result.add(a);
        }
        return result;
    }

    public void cancelAuction(Player player, String auctionId) {
        Auction auction = auctions.get(auctionId);
        if (auction == null) return;
        if (!auction.seller.equals(player.getUniqueId())) return;

        // Return item
        player.getInventory().addItem(auction.item);
        auctions.remove(auctionId);
        player.sendMessage("§aAuktion abgebrochen und Item zurückgegeben.");
    }

    // ==================== CREATE SESSION ====================

    public static class CreateSession {
        public ItemStack item;
        public double price;
        public boolean priceSet;

        public CreateSession() {
            this.item = null;
            this.price = 0;
            this.priceSet = false;
        }
    }

    // ==================== BROWSE SESSION (SEARCH & SORT) ====================

    public enum SortMode {
        NEWEST("ɴᴇᴜᴇsᴛᴇ", "§7Neuste zuerst"),
        OLDEST("äʟᴛᴇsᴛᴇ", "§7Älteste zuerst"),
        PRICE_LOW("ᴘʀᴇɪs ↑", "§7Preis aufsteigend"),
        PRICE_HIGH("ᴘʀᴇɪs ↓", "§7Preis absteigend"),
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
        return createBrowseGUI(page, null);
    }

    public Inventory createBrowseGUI(int page, UUID player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§6§l" + toSmallCaps("AUKTIONSHAUS") + " §8(" + (page + 1) + ")");

        // Fill borders
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName("§8⬛");
        border.setItemMeta(meta);

        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,51,52}) {
            inv.setItem(i, border);
        }

        // Auctions (filtered if player is provided)
        List<Auction> all = (player != null) ? getFilteredAuctions(player) : getAuctions();
        int start = page * 28;
        int end = Math.min(start + 28, all.size());

        int slot = 10;
        for (int i = start; i < end; i++) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;

            Auction auction = all.get(i);
            inv.setItem(slot, createAuctionItem(auction));
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
                searchLore.add("§7Durchsuche Auktionen");
                searchLore.add("§7nach Item-Namen");
                searchLore.add("§8");
                searchLore.add("§e▸ Klicken zum Suchen");
            }
        } else {
            searchLore.add("§7Durchsuche Auktionen");
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
        sortMeta.setDisplayName("§6§l⇅ " + toSmallCaps("SORTIERUNG"));
        List<String> sortLore = new ArrayList<>();
        sortLore.add("§8");
        if (player != null) {
            BrowseSession session = getBrowseSession(player);
            sortLore.add("§7Aktuell: §e" + session.sortMode.display);
            sortLore.add("§8" + session.sortMode.desc);
            sortLore.add("§8");
            sortLore.add("§e▸ Klicken zum Wechseln");
        } else {
            sortLore.add("§7Sortiere Auktionen");
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
        navLore.add("§7Auktionen: §f" + all.size());
        navLore.add("§7Seiten: §f" + ((all.size() + 27) / 28));
        navLore.add("§8");
        if (page > 0) navLore.add("§a« Vorherige Seite (Pfeil links)");
        if (end < all.size()) navLore.add("§a» Nächste Seite (Pfeil rechts)");
        navMeta.setLore(navLore);
        navInfo.setItemMeta(navMeta);
        inv.setItem(49, navInfo);

        // My Auctions Button (Slot 53)
        ItemStack myBtn = mark(new ItemStack(Material.ENDER_CHEST), "my_auctions", null);
        ItemMeta myMeta = myBtn.getItemMeta();
        myMeta.setDisplayName("§e§l⚑ " + toSmallCaps("MEINE AUKTIONEN"));
        List<String> myLore = new ArrayList<>();
        myLore.add("§8");
        myLore.add("§7Zeige deine aktiven");
        myLore.add("§7Auktionen an");
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

    public Inventory createMyAuctionsGUI(UUID player) {
        Inventory inv = Bukkit.createInventory(null, 54, "§e§l" + toSmallCaps("MEINE AUKTIONEN"));

        // Fill borders
        ItemStack border = mark(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), "border", null);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName("§8⬛");
        border.setItemMeta(meta);

        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53}) {
            inv.setItem(i, border);
        }

        // First slot: "Neue Auktion erstellen" Button
        ItemStack newBtn = mark(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "new", null);
        ItemMeta newMeta = newBtn.getItemMeta();
        newMeta.setDisplayName("§a§l+ " + toSmallCaps("NEUE AUKTION"));
        List<String> newLore = new ArrayList<>();
        newLore.add("§8");
        newLore.add("§7Verkaufe Items an andere");
        newLore.add("§7Spieler im Auktionshaus");
        newLore.add("§8");
        newLore.add("§e▸ Klicken zum Erstellen");
        newMeta.setLore(newLore);
        newBtn.setItemMeta(newMeta);
        inv.setItem(10, newBtn);

        // My Auctions (starting from slot 11)
        List<Auction> mine = getPlayerAuctions(player);
        int slot = 11;
        for (Auction auction : mine) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            if (slot >= 44) break;

            ItemStack display = auction.item.clone();
            ItemMeta displayMeta = display.getItemMeta();
            List<String> lore = displayMeta.hasLore() ? new ArrayList<>(displayMeta.getLore()) : new ArrayList<>();
            lore.add("§8");
            lore.add("§7Preis: §e" + NumberFormatter.formatMoney(auction.price));
            lore.add("§8");
            lore.add("§c§l✖ " + toSmallCaps("ABBRECHEN"));
            lore.add("§7Item wird zurückerstattet");
            displayMeta.setLore(lore);
            display.setItemMeta(displayMeta);
            mark(display, "cancel", auction.id);

            inv.setItem(slot, display);
            slot++;
        }

        return inv;
    }

    public Inventory createNewAuctionGUI(UUID player) {
        CreateSession session = createSessions.get(player);
        if (session == null) {
            session = new CreateSession();
            createSessions.put(player, session);
        }

        Inventory inv = Bukkit.createInventory(null, 9, "§a§l" + toSmallCaps("NEUE AUKTION"));

        // Slot 4: Item without modification (free chest slot)
        if (session.item != null) {
            inv.setItem(4, session.item.clone());
        }
        // Slot 4 bleibt leer wenn kein Item

        // Slot 3: Back Button
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
        inv.setItem(3, back);

        // Slot 5: Set Price Button (always active)
        ItemStack price = mark(new ItemStack(Material.GOLD_INGOT), "set_price", null);
        ItemMeta priceMeta = price.getItemMeta();
        if (session.priceSet) {
            priceMeta.setDisplayName("§6§l$ " + toSmallCaps("PREIS ÄNDERN"));
        } else {
            priceMeta.setDisplayName("§6§l$ " + toSmallCaps("PREIS FESTLEGEN"));
        }
        List<String> priceLore = new ArrayList<>();
        priceLore.add("§8");
        if (session.priceSet) {
            priceLore.add("§7Aktueller Preis: §e" + NumberFormatter.formatMoney(session.price));
            priceLore.add("§8");
        }
        priceLore.add("§7Öffnet ein Schild zur");
        priceLore.add("§7Eingabe des Preises");
        priceLore.add("§8");
        priceLore.add("§e▸ Klicken zum Öffnen");
        priceMeta.setLore(priceLore);
        price.setItemMeta(priceMeta);
        inv.setItem(5, price);

        // Slot 8: Confirm Button
        if (session.priceSet && session.item != null) {
            ItemStack confirm = mark(new ItemStack(Material.LIME_STAINED_GLASS_PANE), "confirm", null);
            ItemMeta confirmMeta = confirm.getItemMeta();
            confirmMeta.setDisplayName("§a§l✓ " + toSmallCaps("AUKTION ERSTELLEN"));
            List<String> confirmLore = new ArrayList<>();
            confirmLore.add("§8");
            confirmLore.add("§7Item: §f" + session.item.getType().name());
            confirmLore.add("§7Menge: §f" + session.item.getAmount());
            confirmLore.add("§7Preis: §e" + NumberFormatter.formatMoney(session.price));
            confirmLore.add("§8");
            confirmLore.add("§a▸ Klicken zum Erstellen");
            confirmMeta.setLore(confirmLore);
            confirm.setItemMeta(confirmMeta);
            inv.setItem(8, confirm);
        }

        return inv;
    }

    private ItemStack createAuctionItem(Auction auction) {
        ItemStack display = auction.item.clone();
        ItemMeta meta = display.getItemMeta();
        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("§8");
        lore.add("§8§m                    ");
        lore.add("§6⛃ §7Preis: §e" + NumberFormatter.formatMoney(auction.price));
        lore.add("§b⚑ §7Verkäufer: §f" + Bukkit.getOfflinePlayer(auction.seller).getName());
        lore.add("§8§m                    ");
        lore.add("§8");
        lore.add("§a§l» " + toSmallCaps("KLICKEN ZUM KAUFEN"));
        meta.setLore(lore);
        display.setItemMeta(meta);
        return mark(display, "buy", auction.id);
    }

    private ItemStack mark(ItemStack item, String action, String id) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(UI_KEY, PersistentDataType.STRING, "true");
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        if (id != null) {
            meta.getPersistentDataContainer().set(AUCTION_ID_KEY, PersistentDataType.STRING, id);
        }
        item.setItemMeta(meta);
        return item;
    }

    public boolean isAuctionGUI(String title) {
        return title != null && (
            title.contains("ᴀᴜᴋᴛɪᴏɴsʜᴀᴜs") ||
            title.contains("ᴍᴇɪɴᴇ ᴀᴜᴋᴛɪᴏɴᴇɴ") ||
            title.contains("ɴᴇᴜᴇ ᴀᴜᴋᴛɪᴏɴ")
        );
    }

    public String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
    }

    public String getAuctionId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(AUCTION_ID_KEY, PersistentDataType.STRING);
    }
}
