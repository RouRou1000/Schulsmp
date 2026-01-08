package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.SlayShopGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /slayshop - öffnet den PvP-Shop (Shards)
 */
public class SlayShopCommand implements CommandExecutor {
    private final DonutPlugin plugin;

    public SlayShopCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur Spieler."); return true; }
        Player p = (Player) sender;
        new SlayShopGUI(plugin).open(p);
        return true;
    }
}
