package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /crate give <player> <crateId> <amount>
 */
public class CrateCommand implements CommandExecutor {
    private final DonutPlugin plugin;

    public CrateCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) { sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cNur Spieler können dieses Kommando ohne Argumente verwenden."); return true; }
            new de.coolemod.donut.gui.CrateGUI(plugin).open((Player) sender);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("donut.admin")) { sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cKeine Berechtigung."); return true; }
            plugin.getCrateManager().reload();
            sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§aCrates neu geladen.");
            return true;
        }
        if (!sender.hasPermission("donut.admin")) { sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cKeine Berechtigung."); return true; }
        if (args.length != 4 || !args[0].equalsIgnoreCase("give")) { sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§eUsage: /crate give <player> <crateId> <amount>"); return true; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cSpieler nicht online."); return true; }
        String crateId = args[2]; int amount; try { amount = Integer.parseInt(args[3]); } catch (NumberFormatException e) { sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cUngültige Zahl."); return true; }
        if (!plugin.getCrateManager().hasCrate(crateId)) { sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cKiste nicht gefunden."); return true; }
        target.getInventory().addItem(plugin.getCrateManager().createKey(crateId, amount));
        sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a" + amount + " Schlüssel für Kiste §e" + crateId + " §agegeben.");
        return true;
    }
}