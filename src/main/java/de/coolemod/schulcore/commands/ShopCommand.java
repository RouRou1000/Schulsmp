package de.coolemod.schulcore.commands;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.gui.ShopGUI_NEW;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {
    private final SchulCorePlugin plugin;
    public ShopCommand(SchulCorePlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur Spieler."); return true; }
        Player p = (Player) sender;
        new ShopGUI_NEW(plugin).openMainMenu(p);
        return true;
    }
}
