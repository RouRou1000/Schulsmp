package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.SellMultiGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /sellmulti - Öffnet die Sell-Multiplikator GUI.
 */
public class SellMultiCommand implements CommandExecutor {
    private final DonutPlugin plugin;

    public SellMultiCommand(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§c✗ Nur Spieler können das nutzen.");
            return true;
        }

        new SellMultiGUI(plugin).open(p);
        return true;
    }
}
