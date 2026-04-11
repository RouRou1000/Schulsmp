package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.HelpGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /help - Öffnet die Help-GUI mit allen Commands
 */
public class HelpCommand implements CommandExecutor {
    private final DonutPlugin plugin;

    public HelpCommand(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur Spieler können die Hilfe öffnen.");
            return true;
        }
        new HelpGUI(plugin).open(p);
        return true;
    }
}
