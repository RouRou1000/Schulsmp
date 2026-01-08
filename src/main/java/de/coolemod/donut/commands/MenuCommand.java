package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.MainMenuGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /menu - Öffnet das Hauptmenü
 */
public class MenuCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    
    public MenuCommand(DonutPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Nur Spieler können das Menü öffnen.");
            return true;
        }
        
        Player p = (Player) sender;
        new MainMenuGUI(plugin).open(p);
        return true;
    }
}
