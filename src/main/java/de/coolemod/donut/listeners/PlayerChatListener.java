package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Chat-Listener für Auction/Order-Erstellung via GUI
 */
public class PlayerChatListener implements Listener {
    private final DonutPlugin plugin;

    public PlayerChatListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        String msg = e.getMessage().trim();

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
                        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Nicht genug Geld! Benötigt: §e$" + "%.2f".formatted(total));
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    } else {
                        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Order erstellt! §7(§e" + amount + "x §7für §e$" + "%.2f".formatted(pricePerItem) + "§7/Stück, gesamt §e$" + "%.2f".formatted(total) + "§7)");
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
