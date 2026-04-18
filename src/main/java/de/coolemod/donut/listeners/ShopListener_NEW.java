package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.ShopGUI_NEW;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NEUER Shop Listener - nutzt InventoryHolder für maximale Sicherheit
 * Klick auf Shop-Item öffnet "BUYING" GUI mit Mengen-Buttons
 */
public class ShopListener_NEW implements Listener {
    private final DonutPlugin plugin;
    private static final Map<UUID, String> AWAITING_CUSTOM_AMOUNT = new ConcurrentHashMap<>();

    public ShopListener_NEW(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        if (!(e.getInventory().getHolder() instanceof ShopGUI_NEW)) return;

        Player p = (Player) e.getWhoClicked();

        // KRITISCH: Blockiere ALLES
        e.setCancelled(true);

        // Blockiere Klicks im eigenen Inventar
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) {
            return;
        }

        // Zahlentasten/Drag ähnliche Exploit-Klicks sofort blocken
        if (e.getClick().name().contains("NUMBER") || e.getClick().name().contains("SWAP") || e.getClick().name().contains("DROP")) {
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        NamespacedKey actionKey = new NamespacedKey(plugin, "shop_action");
        if (!clicked.getItemMeta().getPersistentDataContainer().has(actionKey, PersistentDataType.STRING)) {
            // Kein Action-Key = Shop-Item zum Kaufen (oder Display-Item im Buy GUI)
            String title = e.getView().getTitle();
            if (title.contains("BUYING")) return; // Display item in buy GUI
            openItemBuyGUI(p, title, e.getSlot());
            return;
        }

        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        ShopGUI_NEW shop = new ShopGUI_NEW(plugin);

        switch (action) {
            // ===== BUY GUI ACTIONS =====
            case "buy_add_1":
                modifyBuyAmount(p, e.getInventory(), 1);
                break;
            case "buy_add_10":
                modifyBuyAmount(p, e.getInventory(), 10);
                break;
            case "buy_add_16":
                modifyBuyAmount(p, e.getInventory(), 16);
                break;
            case "buy_add_100":
                modifyBuyAmount(p, e.getInventory(), 100);
                break;
            case "buy_set_64":
                setBuyAmount(p, e.getInventory(), 64);
                break;
            case "buy_remove_1":
                modifyBuyAmount(p, e.getInventory(), -1);
                break;
            case "buy_remove_10":
                modifyBuyAmount(p, e.getInventory(), -10);
                break;
            case "buy_remove_16":
                modifyBuyAmount(p, e.getInventory(), -16);
                break;
            case "buy_remove_100":
                modifyBuyAmount(p, e.getInventory(), -100);
                break;
            case "buy_custom_amount":
                startCustomAmountInput(p);
                break;
            case "buy_confirm":
                confirmBuyPurchase(p);
                break;
            case "buy_back":
                goBackFromBuyGUI(p);
                break;

            // ===== MAIN MENU ACTIONS =====
            case "category_food":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openFoodShop(p);
                break;
            case "category_gear":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openGearShop(p);
                break;
            case "category_nether":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openNetherShop(p);
                break;
            case "category_shards":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openShardShop(p);
                break;
            case "category_end":
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                p.sendMessage("§8┃ §5§lEND SHOP §8┃ §cNoch nicht verfügbar!");
                break;
            case "back":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                shop.openMainMenu(p);
                break;
            case "close":
                p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                p.closeInventory();
                break;
        }
    }

    // ==================== BUY GUI ====================

    private void openItemBuyGUI(Player p, String title, int slot) {
        ShopGUI_NEW.ShopItem item = ShopGUI_NEW.getShopItem(title, slot);
        if (item == null) return;

        String category = getCategoryFromTitle(title);
        ShopGUI_NEW.startBuySession(p.getUniqueId(), item, category);

        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        new ShopGUI_NEW(plugin).openBuyGUI(p);
    }

    private String getCategoryFromTitle(String title) {
        if (title.contains("FOOD")) return "FOOD";
        if (title.contains("GEAR")) return "GEAR";
        if (title.contains("NETHER")) return "NETHER";
        if (title.contains("SHARDS")) return "SHARDS";
        return "FOOD";
    }

    private void modifyBuyAmount(Player p, Inventory inv, int delta) {
        ShopGUI_NEW.BuySession session = ShopGUI_NEW.getBuySession(p.getUniqueId());
        if (session == null) return;
        boolean unlimited = session.shopItem.grantsShardBalance;
        int maxStack = unlimited ? Integer.MAX_VALUE : session.shopItem.material.getMaxStackSize();
        session.amount = Math.max(1, Math.min(maxStack, session.amount + delta));
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        new ShopGUI_NEW(plugin).updateBuyGUI(inv, session);
    }

    private void setBuyAmount(Player p, Inventory inv, int amount) {
        ShopGUI_NEW.BuySession session = ShopGUI_NEW.getBuySession(p.getUniqueId());
        if (session == null) return;
        boolean unlimited = session.shopItem.grantsShardBalance;
        int maxStack = unlimited ? Integer.MAX_VALUE : session.shopItem.material.getMaxStackSize();
        session.amount = Math.max(1, Math.min(maxStack, amount));
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        new ShopGUI_NEW(plugin).updateBuyGUI(inv, session);
    }

    private void startCustomAmountInput(Player p) {
        ShopGUI_NEW.BuySession session = ShopGUI_NEW.getBuySession(p.getUniqueId());
        if (session == null) return;
        AWAITING_CUSTOM_AMOUNT.put(p.getUniqueId(), session.shopCategory);
        p.closeInventory();
        p.sendMessage("");
        p.sendMessage("\u00a78\u2503 \u00a7d\u00a7l\u2756 SHARD SHOP \u00a78\u2503 \u00a77Gib die gew\u00fcnschte Menge im \u00a7eChat \u00a77ein!");
        p.sendMessage("\u00a78\u2503 \u00a77Tippe \u00a7cabbrechen \u00a77zum Abbrechen.");
        p.sendMessage("");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        if (!AWAITING_CUSTOM_AMOUNT.containsKey(p.getUniqueId())) return;
        e.setCancelled(true);
        String category = AWAITING_CUSTOM_AMOUNT.remove(p.getUniqueId());
        String msg = e.getMessage().trim();

        if (msg.equalsIgnoreCase("abbrechen") || msg.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                ShopGUI_NEW shop = new ShopGUI_NEW(plugin);
                shop.openShardShop(p);
            });
            p.sendMessage("\u00a78\u2503 \u00a7c\u00a7l\u2716 \u00a77Eingabe abgebrochen.");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(msg);
        } catch (NumberFormatException ex) {
            p.sendMessage("\u00a78\u2503 \u00a7c\u00a7l\u2716 \u00a77Ung\u00fcltige Zahl! Bitte erneut versuchen.");
            AWAITING_CUSTOM_AMOUNT.put(p.getUniqueId(), category);
            return;
        }

        if (amount < 1) {
            p.sendMessage("\u00a78\u2503 \u00a7c\u00a7l\u2716 \u00a77Die Menge muss mindestens 1 sein!");
            AWAITING_CUSTOM_AMOUNT.put(p.getUniqueId(), category);
            return;
        }

        final int finalAmount = amount;
        Bukkit.getScheduler().runTask(plugin, () -> {
            ShopGUI_NEW.BuySession session = ShopGUI_NEW.getBuySession(p.getUniqueId());
            if (session != null) {
                session.amount = finalAmount;
            }
            ShopGUI_NEW shop = new ShopGUI_NEW(plugin);
            shop.openBuyGUI(p);
        });
    }

    private void goBackFromBuyGUI(Player p) {
        ShopGUI_NEW.BuySession session = ShopGUI_NEW.getBuySession(p.getUniqueId());
        ShopGUI_NEW.endBuySession(p.getUniqueId());
        p.playSound(p.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        ShopGUI_NEW shop = new ShopGUI_NEW(plugin);
        if (session != null) {
            switch (session.shopCategory) {
                case "FOOD": shop.openFoodShop(p); break;
                case "GEAR": shop.openGearShop(p); break;
                case "NETHER": shop.openNetherShop(p); break;
                case "SHARDS": shop.openShardShop(p); break;
                default: shop.openMainMenu(p); break;
            }
        } else {
            shop.openMainMenu(p);
        }
    }

    private void confirmBuyPurchase(Player p) {
        ShopGUI_NEW.BuySession session = ShopGUI_NEW.getBuySession(p.getUniqueId());
        if (session == null) return;

        ShopGUI_NEW.ShopItem item = session.shopItem;
        int amount = session.amount;
        double pricePerUnit = (double) item.price / item.amount;
        double totalPrice = amount * pricePerUnit;

        if (item.isShard) {
            // Shard-Kauf
            int requiredShards = (int) Math.ceil(totalPrice);
            int shards = plugin.getShards().getShards(p.getUniqueId());
            if (shards < requiredShards) {
                p.sendMessage("");
                p.sendMessage("§8┃ §c§l✖ SHARD SHOP §8┃ §cNicht genug Shards!");
                p.sendMessage("§8┃ §7Benötigt§8: §d" + NumberFormatter.formatInt(requiredShards) + " Shards");
                p.sendMessage("§8┃ §7Deine Shards§8: §d" + shards);
                p.sendMessage("§8┃ §7Fehlt§8: §c" + NumberFormatter.formatInt(requiredShards - shards) + " Shards");
                p.sendMessage("");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (plugin.getShards().removeShards(p.getUniqueId(), requiredShards)) {
                // Gib Spawner
                if (item.spawnerType != null && item.material == Material.SPAWNER) {
                    try {
                        org.bukkit.entity.EntityType entityType = org.bukkit.entity.EntityType.valueOf(item.spawnerType);
                        ItemStack spawner = plugin.getSpawnerManager().createSpawnerItem(entityType);
                        for (int i = 0; i < amount; i++) {
                            p.getInventory().addItem(spawner.clone());
                        }
                    } catch (Exception ex) {
                        giveItems(p, item.material, amount);
                    }
                } else {
                    giveItems(p, item.material, amount);
                }

                p.sendMessage("");
                p.sendMessage("§8┃ §d§lSHARD SHOP §8┃ §a§l✓ GEKAUFT!");
                p.sendMessage("§8┃ §7Item§8: §f" + item.name + " §8x" + NumberFormatter.formatInt(amount));
                p.sendMessage("§8┃ §7Preis§8: §d" + NumberFormatter.formatInt(requiredShards) + " Shards");
                p.sendMessage("§8┃ §7Neue Shards§8: §d" + plugin.getShards().getShards(p.getUniqueId()));
                p.sendMessage("");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);

                // Refresh GUI
                new ShopGUI_NEW(plugin).updateBuyGUI(p.getOpenInventory().getTopInventory(), session);
            }
        } else {
            // Geld-Kauf
            double balance = plugin.getEconomy().getBalance(p.getUniqueId());
            if (balance < totalPrice) {
                p.sendMessage("");
                p.sendMessage("§8┃ §c§l✖ SCHUL SHOP §8┃ §cNicht genug Geld!");
                p.sendMessage("§8┃ §7Benötigt§8: §e" + NumberFormatter.formatMoney(totalPrice));
                p.sendMessage("§8┃ §7Dein Geld§8: §e" + NumberFormatter.formatMoney(balance));
                p.sendMessage("§8┃ §7Fehlt§8: §c" + NumberFormatter.formatMoney(totalPrice - balance));
                p.sendMessage("");
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }

            if (plugin.getEconomy().withdraw(p.getUniqueId(), totalPrice)) {
                if (item.grantsShardBalance) {
                    plugin.getShards().addShards(p.getUniqueId(), amount);

                    p.sendMessage("");
                    p.sendMessage("§8┃ §d§lSHARD SHOP §8┃ §a§l✓ AUFGELADEN!");
                    p.sendMessage("§8┃ §7Shards§8: §d+" + NumberFormatter.formatInt(amount));
                    p.sendMessage("§8┃ §7Preis§8: §e" + NumberFormatter.formatMoney(totalPrice));
                    p.sendMessage("§8┃ §7Neue Shards§8: §d" + NumberFormatter.formatInt(plugin.getShards().getShards(p.getUniqueId())));
                    p.sendMessage("§8┃ §7Neuer Kontostand§8: §a" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(p.getUniqueId())));
                    p.sendMessage("");
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                } else {
                    giveItems(p, item.material, amount);

                    p.sendMessage("");
                    p.sendMessage("§8┃ §6§lSCHUL SHOP §8┃ §a§l✓ GEKAUFT!");
                    p.sendMessage("§8┃ §7Item§8: §f" + item.name + " §8x" + NumberFormatter.formatInt(amount));
                    p.sendMessage("§8┃ §7Preis§8: §e" + NumberFormatter.formatMoney(totalPrice));
                    p.sendMessage("§8┃ §7Neuer Kontostand§8: §a" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(p.getUniqueId())));
                    p.sendMessage("");
                    p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }

                // Refresh GUI
                new ShopGUI_NEW(plugin).updateBuyGUI(p.getOpenInventory().getTopInventory(), session);
            }
        }
    }

    private void giveItems(Player p, Material material, int totalAmount) {
        int remaining = totalAmount;
        int maxStack = material.getMaxStackSize();
        while (remaining > 0) {
            int stackAmount = Math.min(maxStack, remaining);
            ItemStack give = new ItemStack(material, stackAmount);
            java.util.HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(give);
            if (!leftover.isEmpty()) {
                for (ItemStack left : leftover.values()) {
                    p.getWorld().dropItemNaturally(p.getLocation(), left);
                }
                p.sendMessage("§8┃ §e⚠ Inventar voll! Items gedroppt.");
            }
            remaining -= stackAmount;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof ShopGUI_NEW)) return;
        e.setCancelled(true);
    }
}
