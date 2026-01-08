package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /worth - zeigt Wert des gehaltenen Items
 * /worth reload - (admin) neu laden
 */
public class WorthCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    public WorthCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("donut.admin")) { sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cKeine Berechtigung."); return true; }
            plugin.getWorthManager().reload();
            sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§aWorth-Daten neu geladen.");
            return true;
        }

        if (!(sender instanceof Player)) { sender.sendMessage("Nur Spieler."); return true; }
        Player p = (Player) sender;
        ItemStack inHand = p.getInventory().getItemInMainHand();
        if (inHand == null || inHand.getType().isAir()) { p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§eHalte ein Item in der Hand."); return true; }
        double worth = plugin.getWorthManager().getWorth(inHand);
        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "Wert: §a" + String.format("%.2f", worth) + " §7pro Stück.");
        return true;
    }
}
