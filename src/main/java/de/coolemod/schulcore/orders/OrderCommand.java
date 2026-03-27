package de.coolemod.schulcore.orders;

import de.coolemod.schulcore.SchulCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /order Command - GUI only
 */
public class OrderCommand implements CommandExecutor {
    private final OrderSystem orderSystem;
    
    public OrderCommand(SchulCorePlugin plugin, OrderSystem orderSystem) {
        this.orderSystem = orderSystem;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        
        Player player = (Player) sender;
        // Reset search/sort bei erneutem Öffnen
        orderSystem.clearBrowseSession(player.getUniqueId());
        // Immer GUI, egal welche Argumente
        player.openInventory(orderSystem.createBrowseGUI(0, player.getUniqueId()));
        return true;
    }
}
