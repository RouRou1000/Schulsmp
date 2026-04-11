package de.coolemod.donut.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Blendet normalen Spielern nur die für sie relevanten Befehle ein.
 * Versteckte Commands werden zusätzlich serverseitig blockiert.
 */
public class CommandVisibilityListener implements Listener {
    private static final Set<String> BASE_COMMANDS = Set.of(
        "sell", "verkaufen",
        "worth",
        "pay",
        "balance",
        "shop",
        "ah",
        "order",
        "crate",
        "rtp",
        "help", "hilfe",
        "tutorial",
        "msg", "tell", "w", "whisper",
        "r", "reply",
        "home", "homes", "sethome", "delhome",
        "tpa", "tpahere", "tpaccept", "tpaaccept", "tpdeny", "tpadeny",
        "spawn"
    );

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();

        // Admin/Owner dürfen weiterhin die komplette Command-Liste sehen.
        if (player.hasPermission("donut.admin")) {
            return;
        }

        Set<String> allowed = getAllowedCommands(player);
        event.getCommands().removeIf(command -> !allowed.contains(normalize(command)));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("donut.admin")) {
            return;
        }

        String message = event.getMessage();
        if (message == null || message.length() < 2 || message.charAt(0) != '/') {
            return;
        }

        if (message.startsWith("//")) {
            if (player.hasPermission("worldedit.*")) {
                return;
            }
            event.setCancelled(true);
            player.sendMessage("§8┃ §c§lCOMMAND §8┃ §cDieser Befehl ist für dich nicht verfügbar.");
            return;
        }

        String root = extractRootCommand(message);
        if (root.isEmpty()) {
            return;
        }

        if (isExternallyAllowed(player, root)) {
            return;
        }

        if (!getAllowedCommands(player).contains(normalize(root))) {
            event.setCancelled(true);
            player.sendMessage("§8┃ §c§lCOMMAND §8┃ §cDieser Befehl ist für dich nicht verfügbar.");
        }
    }

    private String normalize(String command) {
        String normalized = command.toLowerCase(Locale.ROOT);
        int namespaceIndex = normalized.indexOf(':');
        if (namespaceIndex >= 0) {
            normalized = normalized.substring(namespaceIndex + 1);
        }
        return normalized;
    }

    private Set<String> getAllowedCommands(Player player) {
        Set<String> allowed = new HashSet<>(BASE_COMMANDS);

        if (player.hasPermission("donut.crate.admin")) {
            allowed.add("crateadmin");
        }
        if (player.hasPermission("donut.money")) {
            allowed.add("money");
        }
        if (player.hasPermission("donut.shards")) {
            allowed.add("shards");
        }
        if (player.hasPermission("donut.ac")) {
            allowed.add("ac");
            allowed.add("packetcheck");
        }
        if (player.hasPermission("donut.wipe")) {
            allowed.add("wipe");
            allowed.add("unwipe");
        }
        if (player.hasPermission("donut.rank.admin")) {
            allowed.add("rank");
        }
        if (player.hasPermission("minecraft.command.teleport")) {
            allowed.add("tp");
            allowed.add("teleport");
        }
        if (player.hasPermission("minecraft.command.gamemode") || player.hasPermission("bukkit.command.gamemode")) {
            allowed.add("gamemode");
            allowed.add("gm");
        }
        if (player.hasPermission("minecraft.command.kick")) {
            allowed.add("kick");
        }
        if (player.hasPermission("minecraft.command.ban")) {
            allowed.add("ban");
            allowed.add("ban-ip");
            allowed.add("banlist");
            allowed.add("pardon");
            allowed.add("pardon-ip");
            allowed.add("unban");
        }
        if (player.hasPermission("worldedit.*")) {
            allowed.add("we");
            allowed.add("worldedit");
        }
        if (player.hasPermission("worldguard.*")
            || player.hasPermission("worldguard.region.claim")
            || player.hasPermission("worldguard.region.define.*")) {
            allowed.add("rg");
            allowed.add("region");
            allowed.add("worldguard");
        }
        if (player.hasPermission("chunky.command.chunky")) {
            allowed.add("chunky");
        }

        return allowed;
    }

    private String extractRootCommand(String message) {
        String withoutSlash = message.substring(1).trim();
        if (withoutSlash.isEmpty()) {
            return "";
        }
        int spaceIndex = withoutSlash.indexOf(' ');
        return spaceIndex >= 0 ? withoutSlash.substring(0, spaceIndex) : withoutSlash;
    }

    private boolean isExternallyAllowed(Player player, String command) {
        String normalized = normalize(command);
        if ((normalized.equals("we") || normalized.equals("worldedit")) && player.hasPermission("worldedit.*")) {
            return true;
        }
        if ((normalized.equals("rg") || normalized.equals("region") || normalized.equals("worldguard"))
            && (player.hasPermission("worldguard.*")
                || player.hasPermission("worldguard.region.claim")
                || player.hasPermission("worldguard.region.define.*"))) {
            return true;
        }
        return normalized.equals("chunky") && player.hasPermission("chunky.command.chunky");
    }
}