package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class MaintenanceCommand implements CommandExecutor, TabCompleter {
    private final DonutPlugin plugin;

    public MaintenanceCommand(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donut.maintenance.manage") && !sender.hasPermission("donut.admin")) {
            sender.sendMessage("§8┃ §6§lMAINTENCE §8┃ §cKeine Berechtigung.");
            return true;
        }

        String name = command.getName().toLowerCase();
        if ("maintence".equals(name)) {
            plugin.getMaintenanceManager().startMaintenance(sender);
            return true;
        }
        if ("unmaintence".equals(name)) {
            plugin.getMaintenanceManager().stopMaintenance(sender);
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }
}