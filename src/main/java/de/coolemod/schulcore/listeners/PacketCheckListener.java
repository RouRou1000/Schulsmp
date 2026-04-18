package de.coolemod.schulcore.listeners;

import de.coolemod.schulcore.SchulCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Überwacht Client-Brand, registrierte Plugin-Channels und identifiziert
 * bekannte Hacked-Clients (Meteor, Wurst, Impact, Aristois etc.).
 */
public class PacketCheckListener implements Listener, PluginMessageListener {

    private static final int DIRECT_HACK_SCORE = 100;
    private static final int STRONG_SIGNAL_SCORE = 70;
    private static final int MEDIUM_SIGNAL_SCORE = 40;
    private static final int LOW_SIGNAL_SCORE = 25;

    private static final Map<String, String> KNOWN_HACKED_BRANDS = new LinkedHashMap<>();
    private static final Map<String, String> KNOWN_HACKED_CHANNELS = new LinkedHashMap<>();
    private static final Map<String, String> KNOWN_SUSPICIOUS_TOKENS = new LinkedHashMap<>();
    private static final Map<String, String> KNOWN_LEGIT_BRANDS = new LinkedHashMap<>();
    private static final Set<String> SAFE_CHANNEL_NAMESPACES = new LinkedHashSet<>();

    static {
        KNOWN_HACKED_BRANDS.put("meteor-client", "§4Meteor Client");
        KNOWN_HACKED_BRANDS.put("meteor", "§4Meteor Client");
        KNOWN_HACKED_BRANDS.put("wurst", "§4Wurst Client");
        KNOWN_HACKED_BRANDS.put("impact", "§4Impact Client");
        KNOWN_HACKED_BRANDS.put("aristois", "§4Aristois");
        KNOWN_HACKED_BRANDS.put("inertia", "§4Inertia Client");
        KNOWN_HACKED_BRANDS.put("kami", "§4Kami Blue");
        KNOWN_HACKED_BRANDS.put("kamiblueclient", "§4Kami Blue");
        KNOWN_HACKED_BRANDS.put("future", "§4Future Client");
        KNOWN_HACKED_BRANDS.put("rusherhack", "§4RusherHack");
        KNOWN_HACKED_BRANDS.put("lambda", "§4Lambda Client");
        KNOWN_HACKED_BRANDS.put("liquidbounce", "§4LiquidBounce");
        KNOWN_HACKED_BRANDS.put("fdpclient", "§4FDP Client");
        KNOWN_HACKED_BRANDS.put("sigmaclient", "§4Sigma Client");
        KNOWN_HACKED_BRANDS.put("sigma", "§4Sigma Client");
        KNOWN_HACKED_BRANDS.put("xulu", "§4Xulu Client");
        KNOWN_HACKED_BRANDS.put("salhack", "§4SalHack");
        KNOWN_HACKED_BRANDS.put("phobos", "§4Phobos Client");
        KNOWN_HACKED_BRANDS.put("konas", "§4Konas Client");
        KNOWN_HACKED_BRANDS.put("cornos", "§4Cornos Client");
        KNOWN_HACKED_BRANDS.put("abyss", "§4Abyss Client");
        KNOWN_HACKED_BRANDS.put("nhack", "§4nHack");
        KNOWN_HACKED_BRANDS.put("hack", "§4Unbekannter Hack-Client");

        KNOWN_HACKED_CHANNELS.put("meteor:client", "§4Meteor Client");
        KNOWN_HACKED_CHANNELS.put("meteor:api", "§4Meteor Client");
        KNOWN_HACKED_CHANNELS.put("meteorclient", "§4Meteor Client");
        KNOWN_HACKED_CHANNELS.put("baritone", "§4Baritone");
        KNOWN_HACKED_CHANNELS.put("wurst:client", "§4Wurst Client");
        KNOWN_HACKED_CHANNELS.put("impact:client", "§4Impact Client");
        KNOWN_HACKED_CHANNELS.put("aristois:client", "§4Aristois");
        KNOWN_HACKED_CHANNELS.put("aristois:mods", "§4Aristois");
        KNOWN_HACKED_CHANNELS.put("inertia:main", "§4Inertia Client");
        KNOWN_HACKED_CHANNELS.put("liquidbounce:client", "§4LiquidBounce");
        KNOWN_HACKED_CHANNELS.put("rusherhack:main", "§4RusherHack");
        KNOWN_HACKED_CHANNELS.put("fdp:client", "§4FDP Client");
        KNOWN_HACKED_CHANNELS.put("xulu:main", "§4Xulu Client");
        KNOWN_HACKED_CHANNELS.put("salhack:main", "§4SalHack");
        KNOWN_HACKED_CHANNELS.put("future:client", "§4Future Client");
        KNOWN_HACKED_CHANNELS.put("konas:client", "§4Konas Client");
        KNOWN_HACKED_CHANNELS.put("lambda:client", "§4Lambda Client");
        KNOWN_HACKED_CHANNELS.put("nhack:main", "§4nHack");

        KNOWN_SUSPICIOUS_TOKENS.put("meteorclient", "§4Meteor Client");
        KNOWN_SUSPICIOUS_TOKENS.put("meteor", "§4Meteor Client");
        KNOWN_SUSPICIOUS_TOKENS.put("baritone", "§4Baritone");
        KNOWN_SUSPICIOUS_TOKENS.put("zergatul", "§eZergatul-Mod");
        KNOWN_SUSPICIOUS_TOKENS.put("freecam", "§eFreecam-Mod");
        KNOWN_SUSPICIOUS_TOKENS.put("invmove", "§eInvMove-Mod");
        KNOWN_SUSPICIOUS_TOKENS.put("killaura", "§4KillAura-Mod");
        KNOWN_SUSPICIOUS_TOKENS.put("autototem", "§4AutoTotem-Mod");
        KNOWN_SUSPICIOUS_TOKENS.put("moddetectorfallback", "§eBypass/Fallback-Mod");

        KNOWN_LEGIT_BRANDS.put("vanilla", "§aVanilla");
        KNOWN_LEGIT_BRANDS.put("lunarclient", "§bLunar Client");
        KNOWN_LEGIT_BRANDS.put("badlion:client", "§bBadlion Client");
        KNOWN_LEGIT_BRANDS.put("labymod", "§bLabyMod");
        KNOWN_LEGIT_BRANDS.put("labymod3", "§bLabyMod 3");
        KNOWN_LEGIT_BRANDS.put("optifine", "§bOptiFine");
        KNOWN_LEGIT_BRANDS.put("feather", "§bFeather Client");
        KNOWN_LEGIT_BRANDS.put("forge", "§6Forge");
        KNOWN_LEGIT_BRANDS.put("fml", "§6Forge");

        Collections.addAll(SAFE_CHANNEL_NAMESPACES,
                "minecraft",
                "badlion",
                "lunarclient",
                "labymod",
                "feather",
                "fml",
                "forge",
                "fabric",
                "fabric-screen-handler-api-v1",
                "fabric-screen-handler-api-v2",
                "fabric-networking-api-v1",
                "fabric-networking-v0");
    }

    private final SchulCorePlugin plugin;
    private final Map<UUID, PlayerClientInfo> clientData = new ConcurrentHashMap<>();

    public PacketCheckListener(SchulCorePlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        clientData.put(p.getUniqueId(), new PlayerClientInfo(p.getName()));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PlayerClientInfo info = clientData.get(p.getUniqueId());
            if (info != null && p.isOnline()) {
                for (String ch : p.getListeningPluginChannels()) {
                    info.channels.add(ch);
                }
                info.analyze();
            }
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> clientData.remove(uuid), 1200L);
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent e) {
        PlayerClientInfo info = clientData.get(e.getPlayer().getUniqueId());
        if (info != null) {
            info.channels.add(e.getChannel());
            info.analyze();
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!"minecraft:brand".equals(channel)) {
            return;
        }
        PlayerClientInfo info = clientData.get(player.getUniqueId());
        if (info == null) {
            return;
        }
        info.brand = decodeBrand(data);
        info.brandReceived = true;
        info.analyze();
    }

    public PlayerClientInfo getClientInfo(UUID uuid) {
        return clientData.get(uuid);
    }

    public Map<UUID, PlayerClientInfo> getAllClientData() {
        return Collections.unmodifiableMap(clientData);
    }

    public void cleanup() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, "minecraft:brand", this);
    }

    public static boolean isSuspiciousChannelName(String channel) {
        String lower = channel == null ? "" : channel.toLowerCase(Locale.ROOT);
        String normalized = normalizeToken(channel);
        for (String token : KNOWN_HACKED_CHANNELS.keySet()) {
            if (matchesToken(lower, normalized, token)) {
                return true;
            }
        }
        for (String token : KNOWN_SUSPICIOUS_TOKENS.keySet()) {
            if (matchesToken(lower, normalized, token)) {
                return true;
            }
        }
        return false;
    }

    private static String decodeBrand(byte[] data) {
        if (data == null || data.length == 0) {
            return "unknown";
        }
        try {
            int value = 0;
            int position = 0;
            int bytesRead = 0;
            byte current;
            do {
                if (position >= data.length) {
                    return new String(data, StandardCharsets.UTF_8).trim();
                }
                current = data[position++];
                value |= (current & 0x7F) << (bytesRead++ * 7);
                if (bytesRead > 5) {
                    return new String(data, StandardCharsets.UTF_8).trim();
                }
            } while ((current & 0x80) != 0);

            if (value >= 0 && position + value <= data.length) {
                return new String(data, position, value, StandardCharsets.UTF_8).trim();
            }
        } catch (Throwable ignored) {
        }
        return new String(data, StandardCharsets.UTF_8).trim();
    }

    private static boolean matchesToken(String rawLower, String normalized, String token) {
        return rawLower.contains(token) || normalized.contains(normalizeToken(token));
    }

    private static String normalizeToken(String value) {
        if (value == null) {
            return "";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder(lower.length());
        for (int i = 0; i < lower.length(); i++) {
            char ch = lower.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9')) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static String getChannelNamespace(String channel) {
        if (channel == null || channel.isBlank()) {
            return "";
        }
        int separator = channel.indexOf(':');
        if (separator < 0) {
            return channel.toLowerCase(Locale.ROOT);
        }
        return channel.substring(0, separator).toLowerCase(Locale.ROOT);
    }

    public static class PlayerClientInfo {
        public final String playerName;
        public String brand = "unbekannt";
        public boolean brandReceived = false;
        public final Set<String> channels = new LinkedHashSet<>();
        public final List<String> detectedClients = new ArrayList<>();
        public final List<String> evidence = new ArrayList<>();
        public boolean suspicious = false;
        public String suspicionReason = "";
        public int suspicionScore = 0;

        public PlayerClientInfo(String playerName) {
            this.playerName = playerName;
        }

        public void analyze() {
            detectedClients.clear();
            evidence.clear();
            suspicious = false;
            suspicionReason = "";
            suspicionScore = 0;

            String brandLower = brand.toLowerCase(Locale.ROOT);
            String normalizedBrand = normalizeToken(brand);

            for (Map.Entry<String, String> entry : KNOWN_HACKED_BRANDS.entrySet()) {
                if (matchesToken(brandLower, normalizedBrand, entry.getKey())) {
                    addSignal(entry.getValue(), "Client-Brand: " + brand, DIRECT_HACK_SCORE);
                }
            }

            for (String ch : channels) {
                String chLower = ch.toLowerCase(Locale.ROOT);
                String normalizedChannel = normalizeToken(ch);

                for (Map.Entry<String, String> entry : KNOWN_HACKED_CHANNELS.entrySet()) {
                    if (matchesToken(chLower, normalizedChannel, entry.getKey())) {
                        addSignal(entry.getValue(), "Channel: " + ch, DIRECT_HACK_SCORE);
                    }
                }

                for (Map.Entry<String, String> entry : KNOWN_SUSPICIOUS_TOKENS.entrySet()) {
                    if (matchesToken(chLower, normalizedChannel, entry.getKey())) {
                        int score = entry.getValue().contains("Meteor") ? STRONG_SIGNAL_SCORE : MEDIUM_SIGNAL_SCORE;
                        addSignal(entry.getValue(), "Verdächtige Namespace-Spur: " + ch, score);
                    }
                }
            }

            for (Map.Entry<String, String> entry : KNOWN_SUSPICIOUS_TOKENS.entrySet()) {
                if (matchesToken(brandLower, normalizedBrand, entry.getKey())) {
                    int score = entry.getValue().contains("Meteor") ? STRONG_SIGNAL_SCORE : MEDIUM_SIGNAL_SCORE;
                    addSignal(entry.getValue(), "Verdächtige Brand-Spur: " + brand, score);
                }
            }

            boolean knownLegitBrand = false;
            for (String legit : KNOWN_LEGIT_BRANDS.keySet()) {
                if (matchesToken(brandLower, normalizedBrand, legit)) {
                    knownLegitBrand = true;
                    break;
                }
            }

            boolean vanillaBrand = matchesToken(brandLower, normalizedBrand, "vanilla");
            boolean fabricBrand = matchesToken(brandLower, normalizedBrand, "fabric");

            if (fabricBrand && detectedClients.isEmpty() && !knownLegitBrand) {
                addSignal("§e⚠ Fabric (verdächtig)", "Fabric-Client ohne bekannte legitime Signatur", LOW_SIGNAL_SCORE);
            }

            if (vanillaBrand) {
                for (String ch : channels) {
                    String namespace = getChannelNamespace(ch);
                    if (!namespace.isEmpty() && !SAFE_CHANNEL_NAMESPACES.contains(namespace)) {
                        addSignal("§eVerdächtiger Vanilla-Spoof", "Vanilla-Brand mit Mod-Channel: " + ch, MEDIUM_SIGNAL_SCORE);
                    }
                }
            }

            if (knownLegitBrand) {
                for (String ch : channels) {
                    String namespace = getChannelNamespace(ch);
                    if (!namespace.isEmpty() && isSuspiciousChannelName(namespace)) {
                        addSignal("§eBrand/Channel-Mismatch", "Legitime Brand mit verdächtigem Namespace: " + ch, MEDIUM_SIGNAL_SCORE);
                    }
                }
            }

            suspicious = suspicionScore >= LOW_SIGNAL_SCORE || !detectedClients.isEmpty();
            suspicionReason = String.join(", ", evidence);
        }

        public String getClientDisplayName() {
            if (!brandReceived) {
                return "§7Warte auf Daten...";
            }
            String brandLower = brand.toLowerCase(Locale.ROOT);
            String normalizedBrand = normalizeToken(brand);

            for (Map.Entry<String, String> entry : KNOWN_LEGIT_BRANDS.entrySet()) {
                if (matchesToken(brandLower, normalizedBrand, entry.getKey())) {
                    return entry.getValue();
                }
            }
            if (!detectedClients.isEmpty()) {
                return detectedClients.get(0);
            }
            if (matchesToken(brandLower, normalizedBrand, "fabric")) {
                return suspicious ? "§eVerdächtiger Fabric-Client" : "§eFabric";
            }
            if (suspicious) {
                return "§eVerdächtiger Client";
            }
            return "§7" + brand;
        }

        private void addSignal(String detectedClient, String reason, int score) {
            if (!detectedClients.contains(detectedClient)) {
                detectedClients.add(detectedClient);
            }
            if (!evidence.contains(reason)) {
                evidence.add(reason);
                suspicionScore = Math.min(100, suspicionScore + score);
            }
        }
    }
}
