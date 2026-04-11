package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.ClanGUI;
import de.coolemod.donut.managers.ClanManager;
import de.coolemod.donut.systems.Clan;
import de.coolemod.donut.systems.ClanPermission;
import org.bukkit.Location;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ClanCommand implements CommandExecutor, TabCompleter {
    private final DonutPlugin plugin;
    private final ClanManager clanManager;
    private final ClanGUI clanGUI;

    public ClanCommand(DonutPlugin plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.clanGUI = new ClanGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler koennen das nutzen.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("c")) {
            return handleDirectClanChat(player, args);
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            Clan clan = clanManager.getClan(player.getUniqueId());
            if (clan == null) {
                sendHelp(player, false);
            } else {
                clanGUI.openMain(player, clan);
            }
            return true;
        }

        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(player, args);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "leave" -> handleLeave(player);
            case "disband" -> handleDisband(player);
            case "kick" -> handleKick(player, args);
            case "chat" -> handleClanChat(player, args);
            case "home" -> handleClanHome(player);
            case "sethome" -> handleSetClanHome(player);
            case "help" -> {
                sendHelp(player, clanManager.isInClan(player.getUniqueId()));
                yield true;
            }
            default -> {
                sendHelp(player, clanManager.isInClan(player.getUniqueId()));
                yield true;
            }
        };
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§8┃ §e§lCLAN §8┃ §7Nutze §e/clan create <name>");
            return true;
        }

        String error = clanManager.validateClanName(player.getUniqueId(), args[1]);
        if (error != null) {
            player.sendMessage(error);
            return true;
        }

        Clan clan = clanManager.createClan(player.getUniqueId(), args[1]);
        player.sendMessage("§8┃ §a§lCLAN §8┃ §7Clan §e" + clan.getName() + " §7erstellt.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
        clanGUI.openMain(player, clan);
        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        Clan clan = clanManager.getClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu bist in keinem Clan.");
            return true;
        }
        if (!clan.hasPermission(player.getUniqueId(), ClanPermission.INVITE_MEMBERS)) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDir fehlt das Recht zum Einladen.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§8┃ §e§lCLAN §8┃ §7Nutze §e/clan invite <spieler>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cSpieler nicht gefunden oder offline.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu kannst dich nicht selbst einladen.");
            return true;
        }
        if (clanManager.isInClan(target.getUniqueId())) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDieser Spieler ist bereits in einem Clan.");
            return true;
        }
        if (!clanManager.addInvite(clan, target.getUniqueId())) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDer Spieler hat bereits eine Einladung.");
            return true;
        }

        player.sendMessage("§8┃ §a§lCLAN §8┃ §e" + target.getName() + " §7wurde eingeladen.");
        target.sendMessage("§8┃ §e§lCLAN §8┃ §7Du wurdest in §e" + clan.getName() + " §7eingeladen.");
        target.sendMessage("§8┃ §7Nutze §e/clan accept " + clan.getName() + " §7zum Beitreten.");
        return true;
    }

    private boolean handleAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§8┃ §e§lCLAN §8┃ §7Nutze §e/clan accept <clan>");
            return true;
        }

        Clan clan = clanManager.acceptInvite(player.getUniqueId(), args[1]);
        if (clan == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cKeine passende Einladung gefunden.");
            return true;
        }

        player.sendMessage("§8┃ §a§lCLAN §8┃ §7Du bist §e" + clan.getName() + " §7beigetreten.");
        Player owner = Bukkit.getPlayer(clan.getOwner());
        if (owner != null) {
            owner.sendMessage("§8┃ §a§lCLAN §8┃ §e" + player.getName() + " §7ist dem Clan beigetreten.");
        }
        clanGUI.openMain(player, clan);
        return true;
    }

    private boolean handleLeave(Player player) {
        Clan clan = clanManager.getClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu bist in keinem Clan.");
            return true;
        }
        if (clan.isOwner(player.getUniqueId())) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cAls Owner musst du den Clan aufloesen.");
            return true;
        }
        if (clanManager.leaveClan(player.getUniqueId())) {
            player.sendMessage("§8┃ §a§lCLAN §8┃ §7Du hast §e" + clan.getName() + " §7verlassen.");
        }
        return true;
    }

    private boolean handleDirectClanChat(Player player, String[] args) {
        Clan clan = clanManager.getClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu bist in keinem Clan.");
            return true;
        }
        if (args.length == 0) {
            player.sendMessage("§8┃ §d§lCLAN CHAT §8┃ §7Nutze §e/c <nachricht>");
            return true;
        }
        sendClanChatMessage(player, clan, String.join(" ", args));
        return true;
    }

    private boolean handleDisband(Player player) {
        Clan clan = clanManager.getClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu bist in keinem Clan.");
            return true;
        }
        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cNur der Owner kann den Clan aufloesen.");
            return true;
        }

        String clanName = clan.getName();
        List<UUID> members = new ArrayList<>(clan.getMembers());
        clanManager.disbandClan(clan);
        for (UUID memberId : members) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage("§8┃ §c§lCLAN §8┃ §7Der Clan §e" + clanName + " §7wurde aufgeloest.");
            }
        }
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        Clan clan = clanManager.getClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu bist in keinem Clan.");
            return true;
        }
        if (!clan.hasPermission(player.getUniqueId(), ClanPermission.KICK_MEMBERS)) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDir fehlt das Recht zum Entfernen von Mitgliedern.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage("§8┃ §e§lCLAN §8┃ §7Nutze §e/clan kick <spieler>");
            return true;
        }

        OfflinePlayer target = findMemberByName(clan, args[1]);
        if (target == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cMitglied nicht gefunden.");
            return true;
        }
        if (clan.isOwner(target.getUniqueId())) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDer Owner kann nicht entfernt werden.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cNutze §e/clan leave §czum Verlassen.");
            return true;
        }

        if (clanManager.kickMember(clan, target.getUniqueId())) {
            player.sendMessage("§8┃ §a§lCLAN §8┃ §e" + clanManager.getPlayerName(target.getUniqueId()) + " §7wurde entfernt.");
            Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
            if (onlineTarget != null) {
                onlineTarget.sendMessage("§8┃ §c§lCLAN §8┃ §7Du wurdest aus §e" + clan.getName() + " §7entfernt.");
            }
        }
        return true;
    }

    private boolean handleClanChat(Player player, String[] args) {
        Clan clan = clanManager.getClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu bist in keinem Clan.");
            return true;
        }

        if (args.length == 1) {
            boolean enabled = clanManager.toggleClanChat(player.getUniqueId());
            player.sendMessage("§8┃ §d§lCLAN CHAT §8┃ §7Status: " + (enabled ? "§aAN" : "§cAUS"));
            return true;
        }

        sendClanChatMessage(player, clan, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
        return true;
    }

    private boolean handleClanHome(Player player) {
        Clan clan = clanManager.getClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu bist in keinem Clan.");
            return true;
        }

        Location home = clan.getClanHome();
        if (home == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cEs ist kein Clan-Home gesetzt.");
            return true;
        }

        plugin.getHomeManager().startTeleport(player, home, "§eClan-Home");
        return true;
    }

    private boolean handleSetClanHome(Player player) {
        Clan clan = clanManager.getClan(player.getUniqueId());
        if (clan == null) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu bist in keinem Clan.");
            return true;
        }
        if (!clan.hasPermission(player.getUniqueId(), ClanPermission.SET_CLAN_HOME)) {
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDir fehlt das Recht dafuer.");
            return true;
        }

        clan.setClanHome(player.getLocation());
        clanManager.save();
        player.sendMessage("§8┃ §a§lCLAN §8┃ §7Clan-Home wurde gesetzt.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        return true;
    }

    private OfflinePlayer findMemberByName(Clan clan, String name) {
        for (UUID memberId : clan.getMembers()) {
            String memberName = clanManager.getPlayerName(memberId);
            if (memberName.equalsIgnoreCase(name)) {
                return Bukkit.getOfflinePlayer(memberId);
            }
        }
        return null;
    }

    private void sendHelp(Player player, boolean inClan) {
        player.sendMessage("§8────────────────");
        player.sendMessage("§6§lCLAN COMMANDS");
        player.sendMessage("§e/clan create <name> §7- Clan erstellen");
        player.sendMessage("§e/clan accept <clan> §7- Einladung annehmen");
        player.sendMessage("§e/c <nachricht> §7- Direkt in Clan-Chat schreiben");
        if (inClan) {
            player.sendMessage("§e/clan §7- Clan GUI oeffnen");
            player.sendMessage("§e/clan invite <spieler> §7- Spieler einladen");
            player.sendMessage("§e/clan kick <spieler> §7- Mitglied entfernen");
            player.sendMessage("§e/clan chat [nachricht] §7- Chat umschalten oder senden");
            player.sendMessage("§e/clan home §7- Clan-Home teleport");
            player.sendMessage("§e/clan sethome §7- Clan-Home setzen");
            player.sendMessage("§e/clan leave §7- Clan verlassen");
            player.sendMessage("§e/clan disband §7- Clan aufloesen (Owner)");
        }
        player.sendMessage("§8────────────────");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        Clan clan = clanManager.getClan(player.getUniqueId());
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            options.add("create");
            options.add("accept");
            options.add("help");
            if (clan != null) {
                options.add("menu");
                options.add("invite");
                options.add("kick");
                options.add("chat");
                options.add("home");
                options.add("sethome");
                options.add("leave");
                if (clan.isOwner(player.getUniqueId())) {
                    options.add("disband");
                }
            }
            return filter(options, args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "invite" -> filter(onlinePlayersNotInClan(player.getUniqueId()), args[1]);
                case "accept" -> filter(clanManager.getInviteClanNames(player.getUniqueId()), args[1]);
                case "kick" -> clan == null ? Collections.emptyList() : filter(clanMemberNames(clan, player.getUniqueId()), args[1]);
                default -> Collections.emptyList();
            };
        }

        return Collections.emptyList();
    }

    private List<String> onlinePlayersNotInClan(UUID selfId) {
        List<String> result = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.getUniqueId().equals(selfId) && !clanManager.isInClan(online.getUniqueId())) {
                result.add(online.getName());
            }
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private List<String> clanMemberNames(Clan clan, UUID selfId) {
        List<String> result = new ArrayList<>();
        for (UUID memberId : clan.getMembers()) {
            if (!memberId.equals(selfId) && !clan.isOwner(memberId)) {
                result.add(clanManager.getPlayerName(memberId));
            }
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    private List<String> filter(List<String> input, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : input) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                result.add(value);
            }
        }
        return result;
    }

    private void sendClanChatMessage(Player sender, Clan clan, String message) {
        String senderName = plugin.getScoreboardManager() != null
            ? plugin.getScoreboardManager().getChatFormat(sender)
            : "§7" + sender.getName();
        String formatted = "§8[§d§lCLAN§8] " + senderName + "§8: §f" + message;
        for (UUID memberId : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }
    }
}