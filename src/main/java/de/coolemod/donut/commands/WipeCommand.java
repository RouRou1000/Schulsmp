package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.WipeManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * /wipe <spieler>   – Wiped alle Daten eines Spielers (Inventar, Ränge, Geld, etc.)
 * /unwipe <spieler>  – Stellt die gewipten Daten wieder her
 */
public class WipeCommand implements CommandExecutor, TabCompleter {

    private final DonutPlugin plugin;
    private final WipeManager wipeManager;

    public WipeCommand(DonutPlugin plugin, WipeManager wipeManager) {
        this.plugin = plugin;
        this.wipeManager = wipeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("donut.wipe")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        boolean isWipe = label.equalsIgnoreCase("wipe");

        if (args.length < 1) {
            if (isWipe) {
                sender.sendMessage("§8[§6§lWIPE§8] §7Nutze: §6/wipe <spieler>");
                sender.sendMessage("§8[§6§lWIPE§8] §7Wiped: Inventar, Enderchest, Geld, Shards, Kills/Deaths, Homes, XP");
            } else {
                sender.sendMessage("§8[§6§lWIPE§8] §7Nutze: §6/unwipe <spieler>");
                sender.sendMessage("§8[§6§lWIPE§8] §7Stellt alle gewipten Daten wieder her.");
            }
            return true;
        }

        String targetName = args[0];
        if (isWipe) {
            handleWipe(sender, targetName);
        } else {
            handleUnwipe(sender, targetName);
        }
        return true;
    }

    private void handleWipe(CommandSender sender, String targetName) {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            sender.sendMessage("§8[§6§lWIPE§8] §cSpieler §e" + targetName + " §cwurde nie auf dem Server gesehen.");
            return;
        }

        String name = target.getName() != null ? target.getName() : targetName;

        if (wipeManager.hasBackup(target.getUniqueId())) {
            sender.sendMessage("§8[§6§lWIPE§8] §7Altes Backup von §e" + name + " §7wird überschrieben.");
        }

        boolean success = wipeManager.wipePlayer(target.getUniqueId(), name, sender.getName());

        if (success) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("donut.wipe")) {
                    p.sendMessage("");
                    p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    p.sendMessage("  §6§lWIPE §8- §7Spieler gewiped");
                    p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    p.sendMessage("  §7Spieler: §f" + name);
                    p.sendMessage("  §7Von: §f" + sender.getName());
                    p.sendMessage("  §7Status: §aBackup erstellt");
                    p.sendMessage("  §7Rückgängig: §f/unwipe " + name);
                    p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            }

            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                onlineTarget.sendMessage("");
                onlineTarget.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                onlineTarget.sendMessage("  §6§lDeine Daten wurden zurückgesetzt!");
                onlineTarget.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                onlineTarget.sendMessage("");
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("§8[§6§lWIPE§8] §a" + name + " §7wurde erfolgreich gewiped.");
            }
        } else {
            sender.sendMessage("§8[§6§lWIPE§8] §cFehler beim Wipen von §e" + name + "§c! Backup konnte nicht erstellt werden.");
        }
    }

    private void handleUnwipe(CommandSender sender, String targetName) {
        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            sender.sendMessage("§8[§6§lWIPE§8] §cSpieler §e" + targetName + " §cwurde nie auf dem Server gesehen.");
            return;
        }

        String name = target.getName() != null ? target.getName() : targetName;

        if (!wipeManager.hasBackup(target.getUniqueId())) {
            sender.sendMessage("§8[§6§lWIPE§8] §cKein Backup für §e" + name + " §cgefunden.");
            return;
        }

        boolean success = wipeManager.unwipePlayer(target.getUniqueId());

        if (success) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("donut.wipe")) {
                    p.sendMessage("");
                    p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    p.sendMessage("  §a§lUNWIPE §8- §7Daten wiederhergestellt");
                    p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    p.sendMessage("  §7Spieler: §f" + name);
                    p.sendMessage("  §7Von: §f" + sender.getName());
                    p.sendMessage("  §7Status: §aDaten wiederhergestellt");
                    p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                }
            }

            Player onlineTarget = target.getPlayer();
            if (onlineTarget != null && onlineTarget.isOnline()) {
                onlineTarget.sendMessage("");
                onlineTarget.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                onlineTarget.sendMessage("  §a§lDeine Daten wurden wiederhergestellt!");
                onlineTarget.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                onlineTarget.sendMessage("");
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage("§8[§6§lWIPE§8] §a" + name + " §7wurde erfolgreich unwiped.");
            }
        } else {
            sender.sendMessage("§8[§6§lWIPE§8] §cFehler beim Unwipe von §e" + name + "§c!");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("donut.wipe")) return Collections.emptyList();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(input))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
