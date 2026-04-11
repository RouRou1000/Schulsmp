package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /shards <spieler>                  – Zeigt Shards eines Spielers
 * /shards set <spieler> <menge>      – Setzt Shards
 * /shards add <spieler> <menge>      – Gibt Shards dazu
 * /shards remove <spieler> <menge>   – Entfernt Shards
 */
public class ShardsCommand implements CommandExecutor, TabCompleter {
    private final DonutPlugin plugin;

    public ShardsCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("donut.shards")) {
            sender.sendMessage("§cDazu hast du keine Rechte.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args);
            case "add" -> handleAdd(sender, args);
            case "remove" -> handleRemove(sender, args);
            default -> {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§8[§b§lShards§8] §cSpieler nicht online: §e" + args[0]);
                    return true;
                }
                int shards = plugin.getShards().getShards(target.getUniqueId());
                sender.sendMessage("§8[§b§lShards§8] §f" + target.getName() + " §7hat §b" + NumberFormatter.formatInt(shards) + " Shards");
            }
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§8[§b§lShards§8] §7Nutze: §e/shards set <spieler> <menge>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§8[§b§lShards§8] §cSpieler nicht online: §e" + args[1]);
            return;
        }
        int amount = (int) NumberFormatter.parse(args[2]);
        if (amount < 0) {
            sender.sendMessage("§8[§b§lShards§8] §cUngültige Zahl: §e" + args[2] + " §7(z.B. 1000, 10k, 1m)");
            return;
        }

        // Berechne Differenz und setze
        int current = plugin.getShards().getShards(target.getUniqueId());
        if (amount > current) {
            plugin.getShards().addShards(target.getUniqueId(), amount - current);
        } else if (amount < current) {
            plugin.getShards().removeShards(target.getUniqueId(), current - amount);
        }
        sender.sendMessage("§8[§b§lShards§8] §f" + target.getName() + " §7hat jetzt §b" + NumberFormatter.formatInt(amount) + " Shards");
        target.sendMessage("§8[§b§lShards§8] §7Deine Shards wurden auf §b" + NumberFormatter.formatInt(amount) + " §7gesetzt.");
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§8[§b§lShards§8] §7Nutze: §e/shards add <spieler> <menge>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§8[§b§lShards§8] §cSpieler nicht online: §e" + args[1]);
            return;
        }
        int amount = (int) NumberFormatter.parse(args[2]);
        if (amount <= 0) {
            sender.sendMessage("§8[§b§lShards§8] §cUngültige Zahl: §e" + args[2] + " §7(z.B. 1000, 10k, 1m)");
            return;
        }
        plugin.getShards().addShards(target.getUniqueId(), amount);
        int newAmount = plugin.getShards().getShards(target.getUniqueId());
        sender.sendMessage("§8[§b§lShards§8] §b" + NumberFormatter.formatInt(amount) + " Shards §7an §f" + target.getName() + " §7gegeben. Neu: §b" + NumberFormatter.formatInt(newAmount));
        target.sendMessage("§8[§b§lShards§8] §7Du hast §b" + NumberFormatter.formatInt(amount) + " Shards §7erhalten.");
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§8[§b§lShards§8] §7Nutze: §e/shards remove <spieler> <menge>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§8[§b§lShards§8] §cSpieler nicht online: §e" + args[1]);
            return;
        }
        int amount = (int) NumberFormatter.parse(args[2]);
        if (amount <= 0) {
            sender.sendMessage("§8[§b§lShards§8] §cUngültige Zahl: §e" + args[2] + " §7(z.B. 1000, 10k, 1m)");
            return;
        }
        boolean ok = plugin.getShards().removeShards(target.getUniqueId(), amount);
        if (!ok) {
            sender.sendMessage("§8[§b§lShards§8] §cSpieler hat nicht genug Shards!");
            return;
        }
        int newAmount = plugin.getShards().getShards(target.getUniqueId());
        sender.sendMessage("§8[§b§lShards§8] §b" + NumberFormatter.formatInt(amount) + " Shards §7von §f" + target.getName() + " §7entfernt. Neu: §b" + NumberFormatter.formatInt(newAmount));
        target.sendMessage("§8[§b§lShards§8] §7Dir wurden §c" + NumberFormatter.formatInt(amount) + " Shards §7abgezogen.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §b§lShards §8- §7Admin Befehle");
        sender.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
        sender.sendMessage("  §e/shards <spieler> §8- §7Shards anzeigen");
        sender.sendMessage("  §e/shards set <spieler> <menge> §8- §7Shards setzen");
        sender.sendMessage("  §e/shards add <spieler> <menge> §8- §7Shards geben");
        sender.sendMessage("  §e/shards remove <spieler> <menge> §8- §7Shards entfernen");
        sender.sendMessage("");
        sender.sendMessage("§b§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("donut.shards")) return Collections.emptyList();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> opts = new java.util.ArrayList<>(List.of("set", "add", "remove"));
            Bukkit.getOnlinePlayers().forEach(p -> opts.add(p.getName()));
            return opts.stream().filter(s -> s.toLowerCase().startsWith(input)).collect(Collectors.toList());
        }
        if (args.length == 2 && List.of("set", "add", "remove").contains(args[0].toLowerCase())) {
            String input = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName).filter(n -> n.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        if (args.length == 3) return List.of("10", "50", "100", "500");
        return Collections.emptyList();
    }
}
