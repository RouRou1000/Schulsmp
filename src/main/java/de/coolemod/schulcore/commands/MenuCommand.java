package de.coolemod.schulcore.commands;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.gui.MainMenuGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /menu - Öffnet das Hauptmenü
 */
public class MenuCommand implements CommandExecutor {
    private final SchulCorePlugin plugin;

    public MenuCommand(SchulCorePlugin plugin) {
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
