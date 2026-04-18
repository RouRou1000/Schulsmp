package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.SettingsGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /settings - Öffnet das Einstellungen-GUI
 */
public class SettingsCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    private final SettingsGUI gui;

    public SettingsCommand(DonutPlugin plugin) {
        this.plugin = plugin;
        this.gui = new SettingsGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }
        gui.open(p);
        return true;
    }

    public SettingsGUI getGUI() {
        return gui;
    }
}
