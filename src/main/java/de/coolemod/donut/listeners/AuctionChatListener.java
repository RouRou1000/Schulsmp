package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.guis.AuctionCreateGUINew;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Listener for auction price input via chat
 */
public class AuctionChatListener implements Listener {
    private final DonutPlugin plugin;
    private final AuctionCreateGUINew createGUI;
    
    public AuctionChatListener(DonutPlugin plugin, AuctionCreateGUINew createGUI) {
        this.plugin = plugin;
        this.createGUI = createGUI;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        // DEPRECATED: Now using Sign GUI instead of chat input
        // This listener is no longer active
        return;
        /*
        Player player = e.getPlayer();
        
        // Check if player is awaiting price input
        if (!createGUI.isAwaitingPriceInput(player.getUniqueId())) {
            return;
        }
        
        // Cancel the chat event
        e.setCancelled(true);
        
        String message = e.getMessage().trim();
        
        try {
            double price = Double.parseDouble(message);
            
            if (price <= 0) {
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                    "§cPreis muss größer als 0 sein!");
                player.sendMessage("§7Versuche es erneut oder schreibe §cabbrechen§7:");
                return;
            }
            
            // Process on main thread
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                createGUI.handlePriceInput(player, price);
            });
            
        } catch (NumberFormatException ex) {
            // Check for cancel
            if (message.equalsIgnoreCase("abbrechen") || message.equalsIgnoreCase("cancel")) {
                player.sendMessage(plugin.getConfig().getString("messages.prefix", "") + 
                    "§cPreiseingabe abgebrochen.");
                // Clear awaiting state
                org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                    createGUI.handlePriceInput(player, -1); // -1 signals cancel
                });
                return;
            }
            
        }
        */
    }
}
