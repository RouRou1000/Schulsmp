package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.guis.AuctionCreateGUI_V2;
import de.coolemod.donut.guis.AuctionGUINew;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /ah - Auktionshaus Command
 */
public class AuctionCommandNew implements CommandExecutor {
    private final DonutPlugin plugin;
    private final AuctionGUINew auctionGUI;
    private final AuctionCreateGUI_V2 createGUI;

    public AuctionCommandNew(DonutPlugin plugin, AuctionGUINew auctionGUI, AuctionCreateGUI_V2 createGUI) {
        this.plugin = plugin;
        this.auctionGUI = auctionGUI;
        this.createGUI = createGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur Spieler können das Auktionshaus nutzen!");
            return true;
        }
        Player p = (Player) sender;
        
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        
        // /ah - Browse auctions (main GUI)
        if (args.length == 0) {
            auctionGUI.open(p, 0);
            return true;
        }
        
        // /ah preis <betrag> - Set price for active auction creation
        if (args.length == 2 && args[0].equalsIgnoreCase("preis")) {
            AuctionCreateGUI_V2.Session session = createGUI.getSession(p.getUniqueId());
            if (session == null) {
                p.sendMessage(prefix + "§cKeine aktive Auktions-Erstellung!");
                return true;
            }
            
            try {
                double price = Double.parseDouble(args[1]);
                createGUI.setPrice(p, price);
            } catch (NumberFormatException e) {
                p.sendMessage(prefix + "§cUngültiger Preis!");
            }
            return true;
        }
        
        // /ah browse - Alternative for browse
        if (args.length == 1 && args[0].equalsIgnoreCase("browse")) {
            auctionGUI.open(p, 0);
            return true;
        }
        
        // /ah meine - Show my auctions
        if (args.length == 1 && (args[0].equalsIgnoreCase("meine") || args[0].equalsIgnoreCase("my"))) {
            auctionGUI.openMyAuctions(p);
            return true;
        }
        
        // Help
        p.sendMessage(prefix + "§7=== §eAuktionshaus Hilfe §7===");
        p.sendMessage("§7/ah §8- §7Öffne Auktionshaus");
        p.sendMessage("§7/ah meine §8- §7Zeige deine Auktionen");
        p.sendMessage("");
        p.sendMessage("§7Nutze die Buttons in der GUI zum");
        p.sendMessage("§7Erstellen und Verwalten von Auktionen!");
        return true;
    }
}
