package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /money <spieler>                  – Zeigt Geld eines Spielers
 * /money set <spieler> <betrag>     – Setzt Geld
 * /money add <spieler> <betrag>     – Gibt Geld dazu
 * /money remove <spieler> <betrag>  – Entfernt Geld
 */
public class MoneyCommand implements CommandExecutor, TabCompleter {
    private final DonutPlugin plugin;

    public MoneyCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("donut.money")) {
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
                // /money <spieler> – Zeigt Balance
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage("§8[§a§lMoney§8] §cSpieler nicht online: §e" + args[0]);
                    return true;
                }
                double bal = plugin.getEconomy().getBalance(target.getUniqueId());
                sender.sendMessage("§8[§a§lMoney§8] §f" + target.getName() + " §7hat §a" + NumberFormatter.formatMoney(bal));
            }
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§8[§a§lMoney§8] §7Nutze: §e/money set <spieler> <betrag>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§8[§a§lMoney§8] §cSpieler nicht online: §e" + args[1]);
            return;
        }
        double amount = NumberFormatter.parse(args[2]);
        if (amount < 0) {
            sender.sendMessage("§8[§a§lMoney§8] §cUngültiger Betrag: §e" + args[2] + " §7(z.B. 1000, 10k, 1.5m)");
            return;
        }
        plugin.getEconomy().set(target.getUniqueId(), amount);
        sender.sendMessage("§8[§a§lMoney§8] §f" + target.getName() + " §7hat jetzt §a" + NumberFormatter.formatMoney(amount));
        target.sendMessage("§8[§a§lMoney§8] §7Dein Geld wurde auf §a" + NumberFormatter.formatMoney(amount) + " §7gesetzt.");
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§8[§a§lMoney§8] §7Nutze: §e/money add <spieler> <betrag>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§8[§a§lMoney§8] §cSpieler nicht online: §e" + args[1]);
            return;
        }
        double amount = NumberFormatter.parse(args[2]);
        if (amount <= 0) {
            sender.sendMessage("§8[§a§lMoney§8] §cUngültiger Betrag: §e" + args[2] + " §7(z.B. 1000, 10k, 1.5m)");
            return;
        }
        plugin.getEconomy().deposit(target.getUniqueId(), amount);
        double newBal = plugin.getEconomy().getBalance(target.getUniqueId());
        sender.sendMessage("§8[§a§lMoney§8] §a" + NumberFormatter.formatMoney(amount) + " §7an §f" + target.getName() + " §7gegeben. Neu: §a" + NumberFormatter.formatMoney(newBal));
        target.sendMessage("§8[§a§lMoney§8] §7Du hast §a" + NumberFormatter.formatMoney(amount) + " §7erhalten.");
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§8[§a§lMoney§8] §7Nutze: §e/money remove <spieler> <betrag>");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§8[§a§lMoney§8] §cSpieler nicht online: §e" + args[1]);
            return;
        }
        double amount = NumberFormatter.parse(args[2]);
        if (amount <= 0) {
            sender.sendMessage("§8[§a§lMoney§8] §cUngültiger Betrag: §e" + args[2] + " §7(z.B. 1000, 10k, 1.5m)");
            return;
        }
        boolean ok = plugin.getEconomy().withdraw(target.getUniqueId(), amount);
        if (!ok) {
            sender.sendMessage("§8[§a§lMoney§8] §cSpieler hat nicht genug Geld!");
            return;
        }
        double newBal = plugin.getEconomy().getBalance(target.getUniqueId());
        sender.sendMessage("§8[§a§lMoney§8] §a" + NumberFormatter.formatMoney(amount) + " §7von §f" + target.getName() + " §7entfernt. Neu: §a" + NumberFormatter.formatMoney(newBal));
        target.sendMessage("§8[§a§lMoney§8] §7Dir wurden §c" + NumberFormatter.formatMoney(amount) + " §7abgezogen.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §a§lMoney §8- §7Admin Befehle");
        sender.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
        sender.sendMessage("  §e/money <spieler> §8- §7Balance anzeigen");
        sender.sendMessage("  §e/money set <spieler> <betrag> §8- §7Geld setzen");
        sender.sendMessage("  §e/money add <spieler> <betrag> §8- §7Geld geben");
        sender.sendMessage("  §e/money remove <spieler> <betrag> §8- §7Geld entfernen");
        sender.sendMessage("");
        sender.sendMessage("§a§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("donut.money")) return Collections.emptyList();
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
        if (args.length == 3) return List.of("100", "1000", "10000");
        return Collections.emptyList();
    }
}
