package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MessageCommand implements CommandExecutor, TabCompleter {
    private final DonutPlugin plugin;
    private final Map<UUID, UUID> lastContacts = new ConcurrentHashMap<>();

    public MessageCommand(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("r")) {
            return handleReply(player, args);
        }

        return handleMessage(player, args);
    }

    private boolean handleMessage(Player sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§8┃ §d§lMSG §8┃ §7Verwendung: §e/msg <spieler> <nachricht>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§8┃ §d§lMSG §8┃ §cSpieler §f" + args[0] + " §cist nicht online!");
            return true;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage("§8┃ §d§lMSG §8┃ §cDu kannst dir nicht selbst schreiben.");
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
        if (message.isEmpty()) {
            sender.sendMessage("§8┃ §d§lMSG §8┃ §cBitte schreibe eine Nachricht.");
            return true;
        }

        sendPrivateMessage(sender, target, message);
        return true;
    }

    private boolean handleReply(Player sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§8┃ §d§lMSG §8┃ §7Verwendung: §e/r <nachricht>");
            return true;
        }

        UUID lastContact = lastContacts.get(sender.getUniqueId());
        if (lastContact == null) {
            sender.sendMessage("§8┃ §d§lMSG §8┃ §cDu hast niemandem zuletzt geschrieben.");
            return true;
        }

        Player target = Bukkit.getPlayer(lastContact);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§8┃ §d§lMSG §8┃ §cDieser Spieler ist nicht mehr online.");
            return true;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage("§8┃ §d§lMSG §8┃ §cDu kannst dir nicht selbst schreiben.");
            return true;
        }

        String message = String.join(" ", args).trim();
        if (message.isEmpty()) {
            sender.sendMessage("§8┃ §d§lMSG §8┃ §cBitte schreibe eine Nachricht.");
            return true;
        }

        sendPrivateMessage(sender, target, message);
        return true;
    }

    private void sendPrivateMessage(Player sender, Player target, String message) {
        String senderName = formatPlayerName(sender);
        String targetName = formatPlayerName(target);

        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("§8┃ §d§lMSG §8┃ §7Du §8→ " + targetName);
        sender.sendMessage("§8┃ §f" + message);
        sender.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        target.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        target.sendMessage("§8┃ §d§lMSG §8┃ " + senderName + " §8→ §7Dir");
        target.sendMessage("§8┃ §f" + message);
        target.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        sender.playSound(sender.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 1.4f);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.6f);

        lastContacts.put(sender.getUniqueId(), target.getUniqueId());
        lastContacts.put(target.getUniqueId(), sender.getUniqueId());
    }

    private String formatPlayerName(Player player) {
        if (plugin.getScoreboardManager() != null) {
            return plugin.getScoreboardManager().getTabName(player);
        }
        return "§7" + player.getName();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (command.getName().equalsIgnoreCase("msg") && args.length == 1) {
            String input = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> !name.equalsIgnoreCase(player.getName()))
                .filter(name -> name.toLowerCase().startsWith(input))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}