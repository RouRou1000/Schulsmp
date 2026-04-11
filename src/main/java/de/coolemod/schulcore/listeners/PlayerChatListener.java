package de.coolemod.schulcore.listeners;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.orders.OrderSystem;
import de.coolemod.schulcore.utils.NumberFormatter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Chat-Listener für Auction/Order-Erstellung via GUI
 */
public class PlayerChatListener implements Listener {
    private final SchulCorePlugin plugin;

    public PlayerChatListener(SchulCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage().trim();

        // NEW OrderSystem: Suche-Eingabe via Chat
        if (p.hasMetadata("order_search_input")) {
            e.setCancelled(true);

            if (msg.equalsIgnoreCase("stop") || msg.equalsIgnoreCase("cancel")) {
                p.removeMetadata("order_search_input", plugin);
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    p.openInventory(plugin.getOrderSystem().createBrowseGUI(0, p.getUniqueId()));
                    p.sendMessage("§8┃ §d§lORDER §8┃ §cSuche abgebrochen.");
                });
                return;
            }

            p.removeMetadata("order_search_input", plugin);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                OrderSystem.BrowseSession session = plugin.getOrderSystem().getBrowseSession(p.getUniqueId());
                session.searchQuery = msg;
                p.openInventory(plugin.getOrderSystem().createBrowseGUI(0, p.getUniqueId()));
                p.sendMessage("§8┃ §d§lORDER §8┃ §7Suche: §e\"" + msg + "\"");
            });
            return;
        }

        // NEW OrderSystem: Menge-Eingabe via Chat
        if (p.hasMetadata("order_input_amount")) {
            e.setCancelled(true);

            if (msg.equalsIgnoreCase("stop") || msg.equalsIgnoreCase("cancel")) {
                p.removeMetadata("order_input_amount", plugin);
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    p.openInventory(plugin.getOrderSystem().createNewOrderGUI(p.getUniqueId()));
                    p.sendMessage("§8┃ §d§lORDER §8┃ §cEingabe abgebrochen.");
                });
                return;
            }

            double parsed = NumberFormatter.parse(msg.replace(",", "."));
            if (parsed <= 0 || parsed > Integer.MAX_VALUE) {
                p.sendMessage("§8┃ §d§lORDER §8┃ §cUngültige Menge! Beispiel: §e64 §7oder §e1k");
                return;
            }

            int amount = (int) Math.floor(parsed);
            if (amount <= 0) {
                p.sendMessage("§8┃ §d§lORDER §8┃ §cMenge muss größer als 0 sein!");
                return;
            }

            p.removeMetadata("order_input_amount", plugin);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                OrderSystem.CreateSession session = plugin.getOrderSystem().getCreateSession(p.getUniqueId());
                if (session == null || session.item == null) {
                    p.sendMessage("§8┃ §d§lORDER §8┃ §cKeine aktive Order-Erstellung.");
                    return;
                }

                session.amount = amount;
                session.amountSet = true;
                p.openInventory(plugin.getOrderSystem().createNewOrderGUI(p.getUniqueId()));
                p.sendMessage("§8┃ §d§lORDER §8┃ §aMenge gesetzt: §f" + NumberFormatter.formatInt(amount));
            });
            return;
        }

        // NEW OrderSystem: Preis-Eingabe via Chat
        if (p.hasMetadata("order_input_price")) {
            e.setCancelled(true);

            if (msg.equalsIgnoreCase("stop") || msg.equalsIgnoreCase("cancel")) {
                p.removeMetadata("order_input_price", plugin);
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    p.openInventory(plugin.getOrderSystem().createNewOrderGUI(p.getUniqueId()));
                    p.sendMessage("§8┃ §d§lORDER §8┃ §cEingabe abgebrochen.");
                });
                return;
            }

            double price = NumberFormatter.parse(msg.replace(",", "."));
            if (price <= 0) {
                p.sendMessage("§8┃ §d§lORDER §8┃ §cUngültiger Preis! Beispiel: §e150 §7oder §e2.5k");
                return;
            }

            p.removeMetadata("order_input_price", plugin);
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                OrderSystem.CreateSession session = plugin.getOrderSystem().getCreateSession(p.getUniqueId());
                if (session == null || session.item == null || !session.amountSet) {
                    p.sendMessage("§8┃ §d§lORDER §8┃ §cBitte zuerst Item und Menge festlegen.");
                    return;
                }

                session.price = price;
                session.priceSet = true;
                p.openInventory(plugin.getOrderSystem().createNewOrderGUI(p.getUniqueId()));
                p.sendMessage("§8┃ §d§lORDER §8┃ §aPreis gesetzt: §e" + NumberFormatter.formatMoney(price) + "§7/Stück");
            });
            return;
        }

        // OLD AUCTION SYSTEM - Now handled by AuctionEventHandler
        // AH Preis-Eingabe
        // if (p.hasMetadata("ah_price_input")) {
        //     e.setCancelled(true);
        //     p.removeMetadata("ah_price_input", plugin);
        //     
        //     try {
        //         double price = Double.parseDouble(msg);
        //         if (price <= 0) {
        //             p.sendMessage("§cPreis muss positiv sein!");
        //             return;
        //         }
        //         
        //         // Speichere Preis
        //         de.coolemod.donut.gui.AuctionCreateGUI.setPrice(p, price);
        //         p.sendMessage("§a✓ Preis gesetzt: §e$" + "%.2f".formatted(price));
        //         
        //         // Öffne GUI wieder
        //         org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
        //             new de.coolemod.donut.gui.AuctionCreateGUI(plugin).open(p);
        //         });
        //     } catch (NumberFormatException ex) {
        //         p.sendMessage("§cNur Zahlen erlaubt!");
        //     }
        //     return;
        // }

        // ==========================================
        //  Rang-Prefix im Chat
        // ==========================================
        de.coolemod.schulcore.managers.RankManager rankManager = plugin.getRankManager();
        if (rankManager != null) {
            String prefix = rankManager.getChatPrefix(p);
            e.setFormat(prefix + "§8 » §f%2$s");
        }

        // Order erstellen (nur Orders, keine Auktionen mehr via Chat!)
        if (p.hasMetadata("order_create_pending")) {
            e.setCancelled(true);
            p.removeMetadata("order_create_pending", plugin);
            
            // Parse: "order 64 10" oder "64 10"
            String[] parts = msg.split("\\s+");
            if (parts.length < 2) {
                p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cFormat: order <Menge> <Preis/Stück>");
                return;
            }
            
            String amountStr = parts.length > 2 ? parts[1] : parts[0];
            String priceStr = parts.length > 2 ? parts[2] : parts[1];
            
            try {
                int amount = Integer.parseInt(amountStr);
                double pricePerItem = Double.parseDouble(priceStr);
                
                if (amount <= 0 || pricePerItem <= 0) {
                    p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cMenge und Preis müssen positiv sein!");
                    return;
                }
                
                ItemStack inHand = p.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()) {
                    p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cHalte ein Item in der Hand!");
                    return;
                }
                
                ItemStack itemType = inHand.clone();
                itemType.setAmount(1);
                
                double total = amount * pricePerItem;
                
                // Create order (sync task)
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    String id = plugin.getOrdersManager().createOrder(p.getUniqueId(), itemType, amount, pricePerItem);
                    if (id == null) {
                        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Nicht genug Geld! Benötigt: §e" + NumberFormatter.formatMoney(total));
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    } else {
                        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Order erstellt! §7(§e" + amount + "x §7für §e" + NumberFormatter.formatMoney(pricePerItem) + "§7/Stück, gesamt §e" + NumberFormatter.formatMoney(total) + "§7)");
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    }
                });
                
            } catch (NumberFormatException ex) {
                p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cUngültige Zahlen! Beispiel: order 64 10");
            }
            return;
        }
    }
}
