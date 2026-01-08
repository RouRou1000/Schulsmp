package de.coolemod.donut.commands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Einfacher TabCompleter: Spieler- und Subcommand-Vorschläge
 */
public class GlobalTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> res = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("crate")) {
            if (args.length == 1) { res.add("give"); }
            else if (args.length == 2) { for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) res.add(p.getName()); }
            else if (args.length == 3) { res.add("1"); res.add("2"); res.add("3"); }
            else if (args.length == 4) { res.add("1"); res.add("5"); res.add("10"); }
        }
        if (command.getName().equalsIgnoreCase("rtp")) {
            if (args.length == 1) {
                for (World w : Bukkit.getWorlds()) {
                    res.add(w.getName());
                }
            }
        }
        return res;
    }
}
