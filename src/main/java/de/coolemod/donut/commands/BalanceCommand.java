package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /balance [spieler] - zeigt Kontostand (eigenen oder von anderem Spieler)
 */
public class BalanceCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    public BalanceCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        
        // /balance <spieler> - Zeige Balance eines anderen Spielers
        if (args.length > 0 && sender.hasPermission("donut.balance.others")) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(prefix + "§c✗ Spieler nicht gefunden.");
                return true;
            }
            double bal = plugin.getEconomy().getBalance(target.getUniqueId());
            int shards = plugin.getShards().getShards(target.getUniqueId());
            int kills = plugin.getStats().getKills(target.getUniqueId());
            sender.sendMessage("");
            sender.sendMessage(prefix + "§e✦ Kontostand von §f" + target.getName() + " §e✦");
            sender.sendMessage("  §7├─ §aGeld: §f$" + String.format("%.2f", bal));
            sender.sendMessage("  §7├─ §dShards: §f" + shards);
            sender.sendMessage("  §7└─ §cKills: §f" + kills);
            sender.sendMessage("");
            return true;
        }
        
        if (!(sender instanceof Player)) { sender.sendMessage("§c✗ Nur Spieler können das nutzen."); return true; }
        Player p = (Player) sender;
        
        double bal = plugin.getEconomy().getBalance(p.getUniqueId());
        int shards = plugin.getShards().getShards(p.getUniqueId());
        int kills = plugin.getStats().getKills(p.getUniqueId());
        int deaths = plugin.getStats().getDeaths(p.getUniqueId());
        double kd = deaths > 0 ? (double) kills / deaths : kills;
        
        p.sendMessage("");
        p.sendMessage(prefix + "§e✦ Dein Kontostand §e✦");
        p.sendMessage("  §7├─ §aGeld: §f$" + String.format("%.2f", bal));
        p.sendMessage("  §7├─ §dShards: §f" + shards);
        p.sendMessage("  §7├─ §cKills: §f" + kills + " §8| §7Deaths: §f" + deaths);
        p.sendMessage("  §7└─ §6K/D: §f" + String.format("%.2f", kd));
        p.sendMessage("");
        p.sendMessage("  §7§oNutze §f/sell §7§ozum Verkaufen von Items.");
        p.sendMessage("");
        return true;
    }
}
