package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
import de.coolemod.donut.gui.WorthGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /worth - öffnet Preis-GUI
 * /worth reload - (admin) neu laden
 */
public class WorthCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    public WorthCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("donut.worth.admin")) {
                sender.sendMessage("§8┃ §e§lWORTH §8┃ §cKeine Berechtigung!");
                return true;
            }
            plugin.getWorthManager().reload();
            sender.sendMessage("§8┃ §e§lWORTH §8┃ §aDaten neu geladen.");
            return true;
        }

        if (!(sender instanceof Player)) { sender.sendMessage("§cDieser Befehl ist nur für Spieler!"); return true; }
        Player p = (Player) sender;

        // /worth - öffnet die Preis-GUI
        new WorthGUI(plugin).open(p, 0);
        return true;
    }
}
