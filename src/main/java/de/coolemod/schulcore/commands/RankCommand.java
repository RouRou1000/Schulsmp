package de.coolemod.schulcore.commands;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.managers.RankManager;
import de.coolemod.schulcore.managers.RankManager.Rank;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /rank set <spieler> <rank> – Setzt den Rang eines Spielers
 * /rank info <spieler>       – Zeigt den Rang eines Spielers
 * /rank list                 – Zeigt alle verfügbaren Ränge
 * /rank reload               – Lädt Ränge neu
 */
public class RankCommand implements CommandExecutor, TabCompleter {

    private final SchulCorePlugin plugin;
    private final RankManager rankManager;

    public RankCommand(SchulCorePlugin plugin, RankManager rankManager) {
        this.plugin = plugin;
        this.rankManager = rankManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("schulcore.rank")) {
            sender.sendMessage("§cDazu hast du keine Rechte.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args);
            case "info" -> handleInfo(sender, args);
            case "list" -> handleList(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!sender.hasPermission("schulcore.rank.set")) {
            sender.sendMessage("§cDazu hast du keine Rechte.");
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§8[§6§lRank§8] §7Nutze: §e/rank set <spieler> <rank>");
            return;
        }

        String targetName = args[1];
        String rankName = args[2].toUpperCase();

        Rank rank;
        try {
            rank = Rank.valueOf(rankName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§8[§6§lRank§8] §cUnbekannter Rang: §e" + args[2]);
            sender.sendMessage("§8[§6§lRank§8] §7Verfügbar: §f" + Arrays.stream(Rank.values())
                    .map(r -> r.getPrefix())
                    .collect(Collectors.joining("§7, ")));
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            sender.sendMessage("§8[§6§lRank§8] §cSpieler §e" + targetName + " §cwurde nie auf dem Server gesehen.");
            return;
        }

        rankManager.setRank(target.getUniqueId(), rank);

        String displayName = target.getName() != null ? target.getName() : targetName;
        sender.sendMessage("§8[§6§lRank§8] §a" + displayName + " §7ist jetzt " + rank.getPrefix() + "§7.");

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            onlineTarget.sendMessage("§8[§6§lRank§8] §7Dein Rang wurde auf " + rank.getPrefix() + " §7gesetzt.");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            if (sender instanceof Player player) {
                Rank rank = rankManager.getRank(player);
                sender.sendMessage("§8[§6§lRank§8] §7Dein Rang: " + rank.getPrefix());
            } else {
                sender.sendMessage("§8[§6§lRank§8] §7Nutze: §e/rank info <spieler>");
            }
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            sender.sendMessage("§8[§6§lRank§8] §cSpieler §e" + args[1] + " §cnicht gefunden.");
            return;
        }

        Rank rank = rankManager.getRank(target.getUniqueId());
        sender.sendMessage("§8[§6§lRank§8] §7Rang von §f" + (target.getName() != null ? target.getName() : args[1]) + "§7: " + rank.getPrefix());
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §6§lVerfügbare Ränge");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
        for (Rank rank : Rank.values()) {
            int permCount = rank.getPermissions().size();
            String permInfo = rank.isOp() ? "§c✦ Alle Rechte (OP)" : "§7" + permCount + " Rechte";
            sender.sendMessage("  " + rank.getPrefix() + " §8(" + rank.name().toLowerCase() + ") §8- " + permInfo);
        }
        sender.sendMessage("");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §6§lRang-System §8- §7Hilfe");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
        sender.sendMessage("  §e/rank set <spieler> <rank> §8- §7Rang setzen");
        sender.sendMessage("  §e/rank info [spieler] §8- §7Rang anzeigen");
        sender.sendMessage("  §e/rank list §8- §7Alle Ränge anzeigen");
        sender.sendMessage("");
        sender.sendMessage("§6§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("schulcore.rank")) return Collections.emptyList();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return List.of("set", "info", "list").stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("info"))) {
            String input = args[1].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            String input = args[2].toLowerCase();
            return Arrays.stream(Rank.values())
                    .map(r -> r.name().toLowerCase())
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
