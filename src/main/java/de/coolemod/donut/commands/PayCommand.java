package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * /pay <spieler> <betrag> - Geld an anderen Spieler senden
 */
public class PayCommand implements CommandExecutor {
    private final DonutPlugin plugin;

    public PayCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        
        if (!(sender instanceof Player)) { sender.sendMessage("§c✗ Nur Spieler."); return true; }
        Player p = (Player) sender;
        
        if (args.length != 2) {
            p.sendMessage("");
            p.sendMessage(prefix + "§e✦ Pay Befehl ✦");
            p.sendMessage("  §7Verwendung: §f/pay <spieler> <betrag>");
            p.sendMessage("  §7Beispiel: §f/pay " + (Bukkit.getOnlinePlayers().isEmpty() ? "Steve" : Bukkit.getOnlinePlayers().iterator().next().getName()) + " 100");
            p.sendMessage("");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            p.sendMessage(prefix + "§c✗ Spieler §f" + args[0] + " §cnicht online.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        if (target.equals(p)) {
            p.sendMessage(prefix + "§c✗ Du kannst dir nicht selbst Geld senden!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            p.sendMessage(prefix + "§c✗ Ungültiger Betrag: §f" + args[1]);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        if (amount <= 0) {
            p.sendMessage(prefix + "§c✗ Der Betrag muss positiv sein.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        if (amount < 1) {
            p.sendMessage(prefix + "§c✗ Mindestbetrag ist §a$1§c.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        double balance = plugin.getEconomy().getBalance(p.getUniqueId());
        if (balance < amount) {
            p.sendMessage(prefix + "§c✗ Nicht genug Geld! Du hast §a$" + "%.2f".formatted(balance));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        // Transaktion durchführen
        plugin.getEconomy().withdraw(p.getUniqueId(), amount);
        plugin.getEconomy().deposit(target.getUniqueId(), amount);
        
        // Feedback
        String amountStr = "%.2f".formatted(amount);
        p.sendMessage("");
        p.sendMessage(prefix + "§a✓ Transaktion erfolgreich!");
        p.sendMessage("  §7Gesendet: §a$" + amountStr + " §7an §f" + target.getName());
        p.sendMessage("  §7Neuer Kontostand: §a$" + "%.2f".formatted(plugin.getEconomy().getBalance(p.getUniqueId())));
        p.sendMessage("");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        
        target.sendMessage("");
        target.sendMessage(prefix + "§a✓ Geld erhalten!");
        target.sendMessage("  §7Von: §f" + p.getName());
        target.sendMessage("  §7Betrag: §a$" + amountStr);
        target.sendMessage("  §7Neuer Kontostand: §a$" + "%.2f".formatted(plugin.getEconomy().getBalance(target.getUniqueId())));
        target.sendMessage("");
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        
        return true;
    }
}