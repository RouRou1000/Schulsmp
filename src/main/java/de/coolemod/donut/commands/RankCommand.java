package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * /rank set <spieler> <rang> – Setzt den Rang eines Spielers (via LuckPerms)
 * /rank info <spieler>       – Zeigt den Rang eines Spielers
 * /rank list                  – Listet alle Ränge auf
 */
public class RankCommand implements CommandExecutor, TabCompleter {

    private final DonutPlugin plugin;

    public RankCommand(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    private LuckPerms getLuckPerms() {
        try {
            return LuckPermsProvider.get();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("donut.rank.admin")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        LuckPerms lp = getLuckPerms();
        if (lp == null) {
            sender.sendMessage("§8[§b§lRANG§8] §cLuckPerms nicht gefunden!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args, lp);
            case "info" -> handleInfo(sender, args, lp);
            case "list" -> handleList(sender, lp);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleSet(CommandSender sender, String[] args, LuckPerms lp) {
        if (args.length < 3) {
            sender.sendMessage("§8[§b§lRANG§8] §7Nutze: §b/rank set <spieler> <rang>");
            return;
        }

        String targetName = args[1];
        String groupName = args[2].toLowerCase();

        Group group = lp.getGroupManager().getGroup(groupName);
        if (group == null) {
            sender.sendMessage("§8[§b§lRANG§8] §cRang §e" + groupName + " §cexistiert nicht!");
            sender.sendMessage("§8[§b§lRANG§8] §7Nutze §b/rank list §7für alle Ränge.");
            return;
        }

        @SuppressWarnings("deprecation")
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("§8[§b§lRANG§8] §cSpieler §e" + targetName + " §cnicht online.");
            return;
        }

        lp.getUserManager().modifyUser(target.getUniqueId(), user -> {
            // Alle alten Gruppen entfernen (außer default)
            user.data().clear(node -> node instanceof InheritanceNode);
            // Neue Gruppe setzen
            user.data().add(InheritanceNode.builder(groupName).build());
            // Primary Group explizit setzen
            user.setPrimaryGroup(groupName);
        });

        sender.sendMessage("§8[§b§lRANG§8] §a" + target.getName() + " §7ist jetzt §b" + group.getFriendlyName() + "§7.");

        // Sidebar + Tab aktualisieren
        if (plugin.getScoreboardManager() != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getScoreboardManager().applyFor(target);
                String tabName = plugin.getScoreboardManager().getTabName(target);
                target.setPlayerListName(tabName);
            }, 5L);
        }
    }

    private void handleInfo(CommandSender sender, String[] args, LuckPerms lp) {
        if (args.length < 2) {
            sender.sendMessage("§8[§b§lRANG§8] §7Nutze: §b/rank info <spieler>");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage("§8[§b§lRANG§8] §cSpieler §e" + targetName + " §cnicht online.");
            return;
        }

        User user = lp.getUserManager().getUser(target.getUniqueId());
        if (user == null) {
            sender.sendMessage("§8[§b§lRANG§8] §cKeine LuckPerms-Daten für §e" + targetName + "§c.");
            return;
        }

        String primaryGroup = user.getPrimaryGroup();
        Group group = lp.getGroupManager().getGroup(primaryGroup);
        String displayName = group != null ? group.getFriendlyName() : primaryGroup;

        sender.sendMessage("");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §b§lRang-Info §8- §7" + target.getName());
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §7Rang: §b" + displayName);
        sender.sendMessage("  §7Gruppe: §f" + primaryGroup);
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void handleList(CommandSender sender, LuckPerms lp) {
        sender.sendMessage("");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §b§lVerfügbare Ränge");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        List<Group> groups = new ArrayList<>(lp.getGroupManager().getLoadedGroups());
        groups.sort((a, b) -> {
            // Sort by weight descending
            int wA = a.getWeight().orElse(0);
            int wB = b.getWeight().orElse(0);
            return Integer.compare(wB, wA);
        });

        for (Group g : groups) {
            int weight = g.getWeight().orElse(0);
            sender.sendMessage("  §8▸ §b" + g.getFriendlyName() + " §8(§7" + g.getName() + "§8) §7Gewicht: §e" + weight);
        }

        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §b§lRang-Verwaltung §8- §7Hilfe");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §b/rank set <spieler> <rang> §8- §7Rang setzen");
        sender.sendMessage("  §b/rank info <spieler> §8- §7Rang-Info anzeigen");
        sender.sendMessage("  §b/rank list §8- §7Alle Ränge anzeigen");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("donut.rank.admin")) return Collections.emptyList();

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
            LuckPerms lp = getLuckPerms();
            if (lp != null) {
                String input = args[2].toLowerCase();
                return lp.getGroupManager().getLoadedGroups().stream()
                        .map(Group::getName)
                        .filter(n -> n.toLowerCase().startsWith(input))
                        .sorted()
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}
