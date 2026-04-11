package de.coolemod.schulcore.commands;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.systems.TpaManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TpaCommand implements CommandExecutor, TabCompleter {
    private final SchulCorePlugin plugin;
    private final TpaManager tpaManager;
    
    public TpaCommand(SchulCorePlugin plugin, TpaManager tpaManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }
        Player player = (Player) sender;
        
        String cmd = command.getName().toLowerCase();
        
        if (cmd.equals("tpa")) {
            if (args.length < 1) {
                player.sendMessage("§8┃ §b§lTPA §8┃ §7Verwendung: §e/tpa <spieler>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§8┃ §b§lTPA §8┃ §cSpieler §f" + args[0] + " §cnicht gefunden!");
                return true;
            }
            tpaManager.sendTpaRequest(player, target);
        } else if (cmd.equals("tpahere")) {
            if (args.length < 1) {
                player.sendMessage("§8┃ §b§lTPA §8┃ §7Verwendung: §e/tpahere <spieler>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage("§8┃ §b§lTPA §8┃ §cSpieler §f" + args[0] + " §cnicht gefunden!");
                return true;
            }
            tpaManager.sendTpaHereRequest(player, target);
        } else if (cmd.equals("tpaccept") || cmd.equals("tpaaccept")) {
            tpaManager.acceptTpa(player);
        } else if (cmd.equals("tpdeny") || cmd.equals("tpadeny")) {
            tpaManager.denyTpa(player);
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return null;
        Player player = (Player) sender;
        
        String cmdName = command.getName().toLowerCase();
        if ((cmdName.equalsIgnoreCase("tpa") || cmdName.equalsIgnoreCase("tpahere")) && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equals(player.getName()))
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
