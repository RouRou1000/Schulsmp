package de.coolemod.donut.auctionhouse;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /ah Command
 */
public class AuctionHouseCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    private final AuctionHouse ah;
    
    public AuctionHouseCommand(DonutPlugin plugin, AuctionHouse ah) {
        this.plugin = plugin;
        this.ah = ah;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            // Open browse GUI with player context
            player.openInventory(ah.createBrowseGUI(0, player.getUniqueId()));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("meine")) {
            // Open my auctions
            player.openInventory(ah.createMyAuctionsGUI(player.getUniqueId()));
            return true;
        }
        
        player.sendMessage("§cNutzung: /ah [meine]");
        return true;
    }
}
