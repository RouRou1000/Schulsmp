package de.coolemod.donut.orders;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /order Command - Öffnet Order GUI
 */
public class OrderCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    private final OrderSystem orderSystem;
    
    public OrderCommand(DonutPlugin plugin, OrderSystem orderSystem) {
        this.plugin = plugin;
        this.orderSystem = orderSystem;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Open browse GUI
            player.openInventory(orderSystem.createBrowseGUI(0));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("meine")) {
            // Open my orders
            player.openInventory(orderSystem.createMyOrdersGUI(player.getUniqueId()));
            return true;
        }
        
        player.sendMessage("§cNutzung: /order [meine]");
        return true;
    }
}
