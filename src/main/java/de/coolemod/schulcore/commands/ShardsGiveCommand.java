package de.coolemod.schulcore.commands;

import de.coolemod.schulcore.SchulCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class ShardsGiveCommand implements CommandExecutor, TabCompleter {
    private final SchulCorePlugin plugin;

    public ShardsGiveCommand(SchulCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cNur Operatoren können diesen Befehl nutzen.");
            return true;
        }
        if (args.length != 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage("§eBenutzung: /shards give <Spieler> <Betrag>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage("§cSpieler nicht gefunden!");
            return true;
        }
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cUngültiger Betrag!");
            return true;
        }
        plugin.getShards().addShards(target.getUniqueId(), amount);
        sender.sendMessage("§a" + amount + " Shards an " + target.getName() + " vergeben.");
        target.sendMessage("§aDu hast " + amount + " Shards erhalten!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("give");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            completions.add("10");
            completions.add("100");
            completions.add("1000");
        }
        return completions;
    }
}
