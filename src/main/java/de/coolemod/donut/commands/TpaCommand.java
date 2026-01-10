package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.systems.TpaManager;
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
    private final DonutPlugin plugin;
    private final TpaManager tpaManager;
    
    public TpaCommand(DonutPlugin plugin, TpaManager tpaManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }
        
        String cmd = command.getName().toLowerCase();
        
        switch (cmd) {
            case "tpa" -> {
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
            }
            case "tpaccept", "tpaaccept" -> {
                tpaManager.acceptTpa(player);
            }
            case "tpdeny", "tpadeny" -> {
                tpaManager.denyTpa(player);
            }
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return null;
        
        if (command.getName().equalsIgnoreCase("tpa") && args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equals(player.getName()))
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
}
