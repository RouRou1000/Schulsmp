package de.coolemod.schulcore.commands;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.listeners.PacketCheckListener;
import de.coolemod.schulcore.listeners.PacketCheckListener.PlayerClientInfo;
import de.coolemod.schulcore.managers.WipeManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * /ac <spieler>                – Zeigt alle Client-Infos (Brand, Channels) eines Spielers
 * /ac bann <spieler> <grund> <zeit> – Bannt einen Spieler mit schöner Nachricht
 * /ac sort                     – Zeigt alle Spieler sortiert, welche von Meteor/Wurst/etc. kommen
 * /ac list                     – Zeigt alle Online-Spieler mit ihrem Client
 * /ac unban <spieler>          – Entbannt einen Spieler
 */
public class PacketCheckCommand implements CommandExecutor, TabCompleter, Listener {

    private final SchulCorePlugin plugin;
    private final PacketCheckListener listener;
    private WipeManager wipeManager;

    private static final List<String> BAN_REASONS = List.of(
            "Hacking", "Cheating", "KillAura", "Fly", "Speed",
            "Nuker", "Xray", "AutoClicker", "Reach", "AntiKnockback",
            "Scaffold", "Timer", "HackedClient", "Meteor", "Wurst",
            "Impact", "LiquidBounce", "Aristois", "FabricHacks"
    );

    private static final List<String> BAN_DURATIONS = List.of(
            "30m", "1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d", "90d", "1y"
    );

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    public PacketCheckCommand(SchulCorePlugin plugin, PacketCheckListener listener) {
        this.plugin = plugin;
        this.listener = listener;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void setWipeManager(WipeManager wipeManager) {
        this.wipeManager = wipeManager;
    }

    // ==========================================
    //  Login-Event: Schöne Ban-Nachricht beim Rejoin
    // ==========================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent e) {
        if (e.getResult() != PlayerLoginEvent.Result.KICK_BANNED) return;

        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        org.bukkit.BanEntry entry = banList.getBanEntry(e.getPlayer().getName());
        if (entry == null) return;

        String reason = entry.getReason() != null ? entry.getReason() : "Kein Grund angegeben";
        String source = entry.getSource() != null ? entry.getSource() : "Server";
        Date expiration = entry.getExpiration();

        String remainingStr;
        String expiresStr;
        if (expiration == null) {
            remainingStr = "§c§lPERMANENT";
            expiresStr = "§cNie";
        } else {
            long remainingMs = expiration.getTime() - System.currentTimeMillis();
            if (remainingMs <= 0) return; // Ban abgelaufen, nicht blockieren
            remainingStr = formatMillis(remainingMs);
            expiresStr = DATE_FORMAT.format(expiration);
        }

        String kickMsg = isBedrockPlayer(e.getPlayer().getName())
            ? buildBedrockBanMessage(reason, remainingStr.replaceAll("§.", ""))
            : buildJavaBanMessage(reason, "Verbleibend", remainingStr, expiresStr, source);

        e.setKickMessage(kickMsg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("schulcore.packetcheck")) {
            sender.sendMessage("§cKeine Berechtigung.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "bann", "ban" -> handleBan(sender, args);
            case "banwipe" -> handleBanWipe(sender, args);
            case "unban", "entbannen" -> handleUnban(sender, args);
            case "sort" -> handleSort(sender);
            case "list" -> handleList(sender);
            case "help" -> sendHelp(sender);
            default -> handlePlayer(sender, args[0]);
        }

        return true;
    }

    // ==========================================
    //  /packetcheck <spieler>
    // ==========================================

    private void handlePlayer(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage("§8[§c§lAC§8] §cSpieler §e" + name + " §cnicht gefunden.");
            return;
        }

        PlayerClientInfo info = listener.getClientInfo(target.getUniqueId());
        if (info == null) {
            sender.sendMessage("§8[§c§lAC§8] §cKeine Daten für §e" + name + " §cvorhanden.");
            return;
        }

        sender.sendMessage("");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §c§lAntiCheat §8- §7" + target.getName());
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
        sender.sendMessage("  §7Client: " + info.getClientDisplayName());
        sender.sendMessage("  §7Brand: §f" + info.brand);
        sender.sendMessage("  §7Ping: §e" + target.getPing() + "ms");
        sender.sendMessage("  §7Verdachts-Score: " + formatSuspicionScore(info.suspicionScore));
        sender.sendMessage("");

        sender.sendMessage("  §e§lRegistrierte Channels §8(" + info.channels.size() + ")§7:");
        if (info.channels.isEmpty()) {
            sender.sendMessage("    §8▸ §7Keine Channels registriert");
        } else {
            List<String> sortedChannels = new ArrayList<>(info.channels);
            sortedChannels.sort(String.CASE_INSENSITIVE_ORDER);
            for (String ch : sortedChannels) {
                String chColor = isHackedChannel(ch) ? "§c" : "§7";
                String marker = isHackedChannel(ch) ? " §4⚠" : "";
                sender.sendMessage("    §8▸ " + chColor + ch + marker);
            }
        }
        sender.sendMessage("");

        if (info.suspicious) {
            sender.sendMessage("  §4§l⚠ VERDÄCHTIG!");
            sender.sendMessage("  §cGrund: §f" + info.suspicionReason);
            if (!info.evidence.isEmpty()) {
                sender.sendMessage("  §cEvidenz:");
                for (String evidence : info.evidence) {
                    sender.sendMessage("    §8▸ §f" + evidence);
                }
            }
            sender.sendMessage("  §cVorschlag: §f/ac bann " + target.getName() + " Hacking 30d");
            if (!info.detectedClients.isEmpty()) {
                sender.sendMessage("  §cErkannte Clients:");
                for (String client : info.detectedClients) {
                    sender.sendMessage("    §8▸ " + client);
                }
            }
        } else {
            sender.sendMessage("  §a✔ Keine bekannten Hacked-Clients erkannt.");
        }

        sender.sendMessage("");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ==========================================
    //  /ac bann <spieler> <grund> <zeit>
    // ==========================================

    private void handleBan(CommandSender sender, String[] args) {
        // /ac bann <spieler> <grund> <zeit>
        if (args.length < 4) {
            sender.sendMessage("§8[§c§lAC§8] §7Nutze: §c/ac bann <spieler> <grund> <zeit>");
            sender.sendMessage("§8[§c§lAC§8] §7Beispiel: §f/ac bann Steve Hacking 30d");
            sender.sendMessage("§8[§c§lAC§8] §7Zeiten: §f30m, 1h, 6h, 12h, 1d, 7d, 14d, 30d, 90d, 1y");
            return;
        }

        String targetName = args[1];
        String durationInput = args[args.length - 1];
        Duration duration = parseDuration(durationInput);
        if (duration == null || duration.isZero() || duration.isNegative()) {
            sender.sendMessage("§8[§c§lAC§8] §cUngültige Zeit: §e" + durationInput);
            sender.sendMessage("§8[§c§lAC§8] §7Erlaubt: §f30m§7, §f1h§7, §f6h§7, §f12h§7, §f1d§7, §f7d§7, §f14d§7, §f30d§7, §f90d§7, §f1y");
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length - 1)).trim();
        if (reason.isEmpty()) {
            sender.sendMessage("§8[§c§lAC§8] §cBitte gib einen Grund an.");
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            sender.sendMessage("§8[§c§lAC§8] §cSpieler §e" + targetName + " §cwurde nie auf dem Server gesehen.");
            return;
        }

        Date expiresAt = Date.from(Instant.now().plus(duration));
        String source = sender.getName();

        Bukkit.getBanList(BanList.Type.NAME).addBan(
                target.getName() != null ? target.getName() : targetName,
                reason, expiresAt, source);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            String kickMsg = isBedrockPlayer(onlineTarget.getName())
                    ? buildBedrockBanMessage(reason, formatDuration(duration))
                    : buildJavaBanMessage(reason, "Dauer", formatDuration(duration), DATE_FORMAT.format(expiresAt), source);
            onlineTarget.kickPlayer(kickMsg);
        }

        // Broadcast an Admins
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("schulcore.packetcheck")) {
                p.sendMessage("");
                p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                p.sendMessage("  §c§lAntiCheat §8- §7Bann");
                p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                p.sendMessage("  §7Spieler: §f" + (target.getName() != null ? target.getName() : targetName));
                p.sendMessage("  §7Grund: §f" + reason);
                p.sendMessage("  §7Dauer: §f" + formatDuration(duration));
                p.sendMessage("  §7Bis: §f" + DATE_FORMAT.format(expiresAt));
                p.sendMessage("  §7Von: §f" + source);
                p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§8[§c§lAC§8] §a" + (target.getName() != null ? target.getName() : targetName) + " §7wurde für §f" + formatDuration(duration) + " §7gebannt.");
        }
    }

    // ==========================================
    //  /ac banwipe <spieler> <grund> <zeit>
    // ==========================================

    private void handleBanWipe(CommandSender sender, String[] args) {
        // /ac banwipe <spieler> <grund> <zeit>
        if (args.length < 4) {
            sender.sendMessage("§8[§c§lAC§8] §7Nutze: §c/ac banwipe <spieler> <grund> <zeit>");
            sender.sendMessage("§8[§c§lAC§8] §7Beispiel: §f/ac banwipe Steve Hacking 30d");
            sender.sendMessage("§8[§c§lAC§8] §7Bannt UND wiped den Spieler (Inventar, Geld, Rang, etc.)");
            return;
        }

        String targetName = args[1];
        String durationInput = args[args.length - 1];
        Duration duration = parseDuration(durationInput);
        if (duration == null || duration.isZero() || duration.isNegative()) {
            sender.sendMessage("§8[§c§lAC§8] §cUngültige Zeit: §e" + durationInput);
            sender.sendMessage("§8[§c§lAC§8] §7Erlaubt: §f30m§7, §f1h§7, §f6h§7, §f12h§7, §f1d§7, §f7d§7, §f14d§7, §f30d§7, §f90d§7, §f1y");
            return;
        }

        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length - 1)).trim();
        if (reason.isEmpty()) {
            sender.sendMessage("§8[§c§lAC§8] §cBitte gib einen Grund an.");
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target.getName() == null && !target.hasPlayedBefore()) {
            sender.sendMessage("§8[§c§lAC§8] §cSpieler §e" + targetName + " §cwurde nie auf dem Server gesehen.");
            return;
        }

        String name = target.getName() != null ? target.getName() : targetName;

        // Wipe durchführen
        if (wipeManager != null) {
            if (wipeManager.hasBackup(target.getUniqueId())) {
                sender.sendMessage("§8[§c§lAC§8] §7Hinweis: Spieler war bereits gewiped. Altes Backup wird überschrieben.");
            }
            boolean wiped = wipeManager.wipePlayer(target.getUniqueId(), name, sender.getName());
            if (!wiped) {
                sender.sendMessage("§8[§c§lAC§8] §cWipe fehlgeschlagen! Bann wird trotzdem durchgeführt.");
            }
        } else {
            sender.sendMessage("§8[§c§lAC§8] §cWipeManager nicht verfügbar! Nur Bann wird durchgeführt.");
        }

        // Bann durchführen
        Date expiresAt = Date.from(Instant.now().plus(duration));
        String source = sender.getName();

        Bukkit.getBanList(BanList.Type.NAME).addBan(name, reason, expiresAt, source);

        Player onlineTarget = target.getPlayer();
        if (onlineTarget != null && onlineTarget.isOnline()) {
            String kickMsg = isBedrockPlayer(onlineTarget.getName())
                    ? buildBedrockBanMessage(reason, formatDuration(duration))
                    : buildJavaBanMessage(reason, "Dauer", formatDuration(duration), DATE_FORMAT.format(expiresAt), source);
            onlineTarget.kickPlayer(kickMsg);
        }

        // Broadcast an Admins
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("schulcore.packetcheck")) {
                p.sendMessage("");
                p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                p.sendMessage("  §c§lAntiCheat §8- §4§lBann + Wipe");
                p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                p.sendMessage("  §7Spieler: §f" + name);
                p.sendMessage("  §7Grund: §f" + reason);
                p.sendMessage("  §7Dauer: §f" + formatDuration(duration));
                p.sendMessage("  §7Bis: §f" + DATE_FORMAT.format(expiresAt));
                p.sendMessage("  §7Von: §f" + source);
                p.sendMessage("  §4§lDaten wurden gewiped!");
                p.sendMessage("  §7Rückgängig: §f/unwipe " + name);
                p.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            }
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("§8[§c§lAC§8] §a" + name + " §7wurde für §f" + formatDuration(duration) + " §7gebannt und gewiped.");
        }
    }

    // ==========================================
    //  /ac unban <spieler>
    // ==========================================

    private void handleUnban(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§8[§c§lAC§8] §7Nutze: §c/ac unban <spieler>");
            return;
        }

        String targetName = args[1];
        BanList banList = Bukkit.getBanList(BanList.Type.NAME);
        if (banList.getBanEntry(targetName) == null) {
            sender.sendMessage("§8[§c§lAC§8] §e" + targetName + " §7ist nicht gebannt.");
            return;
        }

        banList.pardon(targetName);
        sender.sendMessage("§8[§c§lAC§8] §a" + targetName + " §7wurde entbannt.");
    }

    // ==========================================
    //  /packetcheck sort
    // ==========================================

    private void handleSort(CommandSender sender) {
        Map<UUID, PlayerClientInfo> allData = listener.getAllClientData();

        // Nur Online-Spieler
        List<Map.Entry<UUID, PlayerClientInfo>> entries = allData.entrySet().stream()
            .filter(e -> Bukkit.getPlayer(e.getKey()) != null)
            .sorted((a, b) -> {
                // Hohe Scores zuerst, dann nach Client-Name
                int suspA = a.getValue().suspicionScore;
                int suspB = b.getValue().suspicionScore;
                if (suspB != suspA) return Integer.compare(suspB, suspA);
                return a.getValue().playerName.compareToIgnoreCase(b.getValue().playerName);
            })
            .collect(Collectors.toList());

        if (entries.isEmpty()) {
            sender.sendMessage("§8[§c§lPC§8] §7Keine Spielerdaten vorhanden.");
            return;
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m                                                    ");
        sender.sendMessage("  §c§lPacketCheck §8- §7Client-Übersicht (sortiert)");
        sender.sendMessage("§8§m                                                    ");
        sender.sendMessage("");

        // Verdächtige Spieler
        List<Map.Entry<UUID, PlayerClientInfo>> suspicious = entries.stream()
            .filter(e -> e.getValue().suspicious)
            .collect(Collectors.toList());

        if (!suspicious.isEmpty()) {
            sender.sendMessage("  §4§l⚠ Verdächtige Spieler (" + suspicious.size() + "):");
            sender.sendMessage("");
            for (Map.Entry<UUID, PlayerClientInfo> e : suspicious) {
                PlayerClientInfo info = e.getValue();
                StringBuilder line = new StringBuilder();
                line.append("    §c").append(info.playerName);
                line.append(" §8→ ");
                if (!info.detectedClients.isEmpty()) {
                    line.append(String.join("§7, ", info.detectedClients));
                } else {
                    line.append("§eVerdächtig");
                }
                sender.sendMessage(line.toString());
                sender.sendMessage("      §7Score: " + formatSuspicionScore(info.suspicionScore));
                sender.sendMessage("      §7Brand: §f" + info.brand);
                if (!info.suspicionReason.isEmpty()) {
                    sender.sendMessage("      §7Grund: §f" + info.suspicionReason);
                }
            }
            sender.sendMessage("");
        } else {
            sender.sendMessage("  §a✔ Keine verdächtigen Spieler erkannt!");
            sender.sendMessage("");
        }

        // Alle Spieler nach Client sortiert
        // Gruppiere nach erkanntem Client
        Map<String, List<String>> clientGroups = new LinkedHashMap<>();
        for (Map.Entry<UUID, PlayerClientInfo> e : entries) {
            PlayerClientInfo info = e.getValue();
            String clientName = info.getClientDisplayName();
            clientGroups.computeIfAbsent(clientName, k -> new ArrayList<>()).add(info.playerName);
        }

        sender.sendMessage("  §e§lAlle Clients:");
        sender.sendMessage("");
        for (Map.Entry<String, List<String>> group : clientGroups.entrySet()) {
            String players = String.join("§7, §f", group.getValue());
            sender.sendMessage("    " + group.getKey() + " §8(" + group.getValue().size() + ")§7:");
            sender.sendMessage("      §f" + players);
        }

        sender.sendMessage("");
        sender.sendMessage("§8§m                                                    ");
    }

    private boolean isBedrockPlayer(String playerName) {
        return playerName != null && playerName.startsWith(".");
    }

    private String buildBedrockBanMessage(String reason, String timeValue) {
        return "Gebannt\n"
                + "Grund: " + reason + "\n"
                + "Zeit: " + timeValue + "\n"
                + "Discord: discord.gg/9c8KZh49tU";
    }

    private String buildJavaBanMessage(String reason, String timeLabel, String timeValue, String expiresStr, String source) {
        return "\n"
                + "§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                + "\n"
                + "§c§l   SCHULSMP ANTICHEAT\n"
                + "\n"
                + "§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                + "\n"
                + "§7Du wurdest vom Server §cgebannt§7.\n"
                + "\n"
                + "§7Grund: §f" + reason + "\n"
                + "§7" + timeLabel + ": §f" + timeValue + "\n"
                + "§7Läuft ab: §f" + expiresStr + "\n"
                + "§7Gebannt von: §f" + source + "\n"
                + "\n"
                + "§8Wenn das ein Fehler war, erstelle ein Ticket:\n"
                + "§b§ndiscord.gg/9c8KZh49tU\n"
                + "\n"
                + "§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n";
    }

    // ==========================================
    //  /packetcheck list
    // ==========================================

    private void handleList(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§8§m                                                    ");
        sender.sendMessage("  §c§lPacketCheck §8- §7Alle Spieler");
        sender.sendMessage("§8§m                                                    ");
        sender.sendMessage("");

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator
                .comparing((Player p) -> {
                    PlayerClientInfo info = listener.getClientInfo(p.getUniqueId());
                    return info != null && info.suspicious ? 0 : 1;
                })
                .thenComparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        for (Player p : players) {
            PlayerClientInfo info = listener.getClientInfo(p.getUniqueId());
            if (info == null) {
                sender.sendMessage("  §7" + p.getName() + " §8→ §7Keine Daten");
                continue;
            }

            String status = info.suspicious ? "§4⚠ " : "§a✔ ";
            sender.sendMessage("  " + status + "§f" + p.getName()
                + " §8→ " + info.getClientDisplayName()
                + " §8(§7" + info.brand + "§8)"
                + " §8[§7" + p.getPing() + "ms§8]"
                + " §8[" + formatSuspicionScore(info.suspicionScore) + "§8]");
        }

        sender.sendMessage("");
        sender.sendMessage("  §7Gesamt: §e" + Bukkit.getOnlinePlayers().size() + " Spieler");
        sender.sendMessage("§8§m                                                    ");
    }

    // ==========================================
    //  Hilfe
    // ==========================================

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("  §c§lAntiCheat §8- §7Hilfe");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        sender.sendMessage("");
        sender.sendMessage("  §c/ac <spieler> §8- §7Prüft einen Spieler");
        sender.sendMessage("  §c/ac bann <spieler> <grund> <zeit> §8- §7Bannt einen Spieler");
        sender.sendMessage("  §c/ac banwipe <spieler> <grund> <zeit> §8- §7Bannt + Wiped");
        sender.sendMessage("  §c/ac unban <spieler> §8- §7Entbannt einen Spieler");
        sender.sendMessage("  §c/ac sort §8- §7Sortiert nach Hacked-Clients");
        sender.sendMessage("  §c/ac list §8- §7Alle Spieler mit Client");
        sender.sendMessage("  §c/ac help §8- §7Diese Hilfe");
        sender.sendMessage("");
        sender.sendMessage("  §7Erkennt: §4Meteor§7, §4Wurst§7, §4Impact§7, §4Aristois§7,");
        sender.sendMessage("  §4LiquidBounce§7, §4RusherHack§7, §4Future§7, §4Sigma §7u.v.m.");
        sender.sendMessage("");
        sender.sendMessage("§c§l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    // ==========================================
    //  Tab Completer
    // ==========================================

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("schulcore.packetcheck")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(List.of("sort", "list", "help", "bann", "banwipe", "unban"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
            String input = args[0].toLowerCase();
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        }

        // /ac bann|banwipe <spieler> <grund> <zeit>
        if (args[0].equalsIgnoreCase("bann") || args[0].equalsIgnoreCase("banwipe")) {
            if (args.length == 2) {
                // Spielernamen vorschlagen
                String input = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(input))
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .collect(Collectors.toList());
            }
            if (args.length == 3) {
                // Gründe vorschlagen
                String input = args[2].toLowerCase();
                return BAN_REASONS.stream()
                        .filter(s -> s.toLowerCase().startsWith(input))
                        .collect(Collectors.toList());
            }
            if (args.length == 4) {
                // Dauer vorschlagen
                String input = args[3].toLowerCase();
                return BAN_DURATIONS.stream()
                        .filter(s -> s.startsWith(input))
                        .collect(Collectors.toList());
            }
        }

        // /ac unban <spieler>
        if (args[0].equalsIgnoreCase("unban") && args.length == 2) {
            String input = args[1].toLowerCase();
            return Bukkit.getBanList(BanList.Type.NAME).getBanEntries().stream()
                    .map(e -> e.getTarget())
                    .filter(Objects::nonNull)
                    .filter(n -> n.toLowerCase().startsWith(input))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    // ==========================================
    //  Helfer
    // ==========================================

    private static boolean isHackedChannel(String channel) {
        return PacketCheckListener.isSuspiciousChannelName(channel);
    }

    private String formatSuspicionScore(int score) {
        if (score >= 80) {
            return "§4" + score;
        }
        if (score >= 40) {
            return "§e" + score;
        }
        if (score > 0) {
            return "§6" + score;
        }
        return "§a0";
    }

    private Duration parseDuration(String input) {
        String lower = input.toLowerCase(Locale.ROOT).trim();
        if (lower.isEmpty()) return null;

        try {
            if (lower.endsWith("mo")) {
                long value = Long.parseLong(lower.substring(0, lower.length() - 2));
                return Duration.ofDays(value * 30L);
            }
            char unit = lower.charAt(lower.length() - 1);
            long value = Long.parseLong(lower.substring(0, lower.length() - 1));
            return switch (unit) {
                case 'm' -> Duration.ofMinutes(value);
                case 'h' -> Duration.ofHours(value);
                case 'd' -> Duration.ofDays(value);
                case 'w' -> Duration.ofDays(value * 7L);
                case 'y' -> Duration.ofDays(value * 365L);
                default -> null;
            };
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        if (days >= 365 && days % 365 == 0) return (days / 365) + " Jahr(e)";
        if (days >= 30 && days % 30 == 0) return (days / 30) + " Monat(e)";
        if (days >= 7 && days % 7 == 0) return (days / 7) + " Woche(n)";
        if (days > 0) return days + " Tag(e)";
        long hours = duration.toHours();
        if (hours > 0) return hours + " Stunde(n)";
        long minutes = duration.toMinutes();
        return Math.max(minutes, 1) + " Minute(n)";
    }

    private String formatMillis(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " Tag" : " Tage");
        if (hours > 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " Stunde" : " Stunden");
        }
        if (minutes > 0 && days == 0) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " Minute" : " Minuten");
        }
        if (sb.isEmpty()) sb.append("< 1 Minute");
        return sb.toString();
    }
}
