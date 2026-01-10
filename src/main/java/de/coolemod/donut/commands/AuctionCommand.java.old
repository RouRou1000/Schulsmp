package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /ah - Auktionshaus (einfacher Platzhalter für GUI)
 */
public class AuctionCommand implements CommandExecutor {
    private final DonutPlugin plugin;

    public AuctionCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { 
            sender.sendMessage("§cNur Spieler können das Auktionshaus nutzen!"); 
            return true; 
        }
        Player p = (Player) sender;
        
        // Haupt-Befehl: öffne Browse-GUI (Auktionen ansehen)
        if (args.length == 0) {
            new de.coolemod.donut.gui.AuctionGUI(plugin).open(p);
            return true;
        }
        
        // /ah create - Erstelle Auktion
        if (args.length == 1 && args[0].equalsIgnoreCase("create")) {
            new de.coolemod.donut.gui.AuctionCreateGUI(plugin).open(p);
            return true;
        }
        
        // /ah browse - Alternative für browse
        if (args.length == 1 && args[0].equalsIgnoreCase("browse")) {
            new de.coolemod.donut.gui.AuctionGUI(plugin).open(p);
            return true;
        }
        
        // Hilfe
        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§7Nutze §e/ah §7zum Browsen oder klicke auf §e'Auktion Erstellen'");
        return true;
    }
}