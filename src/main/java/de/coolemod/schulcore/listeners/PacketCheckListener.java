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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Überwacht Client-Brand, registrierte Plugin-Channels und identifiziert
 * bekannte Hacked-Clients (Meteor, Wurst, Impact, Aristois etc.)
 */
public class PacketCheckListener implements Listener, PluginMessageListener {

    private final SchulCorePlugin plugin;

    // Pro Spieler gespeicherte Daten
    private final Map<UUID, PlayerClientInfo> clientData = new ConcurrentHashMap<>();

    // ==========================================
    //  Bekannte Client-Signaturen
    // ==========================================

    /** Bekannte Hacked-Client Brands (lowercase) → Anzeigename */
    private static final Map<String, String> KNOWN_HACKED_BRANDS = new LinkedHashMap<>();
    /** Bekannte Hacked-Client Channels (lowercase) → Anzeigename */
    private static final Map<String, String> KNOWN_HACKED_CHANNELS = new LinkedHashMap<>();
    /** Bekannte legitime Client Brands (lowercase) → Anzeigename */
    private static final Map<String, String> KNOWN_LEGIT_BRANDS = new LinkedHashMap<>();

    static {
        // --- Hacked Client Brands ---
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

        // --- Hacked Client Channels ---
        KNOWN_HACKED_CHANNELS.put("meteor:client", "§4Meteor Client");
        KNOWN_HACKED_CHANNELS.put("meteor:api", "§4Meteor Client");
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

        // --- Legit Brands ---
        KNOWN_LEGIT_BRANDS.put("vanilla", "§aVanilla");
        KNOWN_LEGIT_BRANDS.put("lunarclient", "§bLunar Client");
        KNOWN_LEGIT_BRANDS.put("badlion:client", "§bBadlion Client");
        KNOWN_LEGIT_BRANDS.put("labymod", "§bLabyMod");
        KNOWN_LEGIT_BRANDS.put("labymod3", "§bLabyMod 3");
        KNOWN_LEGIT_BRANDS.put("optifine", "§bOptiFine");
        KNOWN_LEGIT_BRANDS.put("feather", "§bFeather Client");
    }

    public PacketCheckListener(SchulCorePlugin plugin) {
        this.plugin = plugin;
        // Brand-Channel registrieren
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", this);
    }

    // ==========================================
    //  Events
    // ==========================================

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        clientData.put(p.getUniqueId(), new PlayerClientInfo(p.getName()));

        // Bereits registrierte Channels beim Join erfassen (verzögert, da manche erst später kommen)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PlayerClientInfo info = clientData.get(p.getUniqueId());
            if (info != null && p.isOnline()) {
                for (String ch : p.getListeningPluginChannels()) {
                    info.channels.add(ch);
                }
                info.analyze();
            }
        }, 40L); // 2 Sekunden warten
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Daten 60s nach Quit behalten (für nachträgliches Checken)
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

    // ==========================================
    //  Plugin Message (Brand)
    // ==========================================

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!"minecraft:brand".equals(channel)) return;

        PlayerClientInfo info = clientData.get(player.getUniqueId());
        if (info == null) return;

        // Brand dekodieren (erstes Byte = Länge in varint, Rest = String)
        String brand;
        try {
            if (data.length > 1 && data[0] == (byte)(data.length - 1)) {
                brand = new String(data, 1, data.length - 1, StandardCharsets.UTF_8);
            } else {
                brand = new String(data, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            brand = "unknown";
        }

        info.brand = brand.trim();
        info.brandReceived = true;
        info.analyze();
    }

    // ==========================================
    //  Zugriff
    // ==========================================

    public PlayerClientInfo getClientInfo(UUID uuid) {
        return clientData.get(uuid);
    }

    public Map<UUID, PlayerClientInfo> getAllClientData() {
        return Collections.unmodifiableMap(clientData);
    }

    public void cleanup() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(plugin, "minecraft:brand", this);
    }

    // ==========================================
    //  Daten-Klasse
    // ==========================================

    public static class PlayerClientInfo {
        public final String playerName;
        public String brand = "unbekannt";
        public boolean brandReceived = false;
        public final Set<String> channels = new LinkedHashSet<>();
        public final List<String> detectedClients = new ArrayList<>();
        public boolean suspicious = false;
        public String suspicionReason = "";

        public PlayerClientInfo(String playerName) {
            this.playerName = playerName;
        }

        /** Analysiert Brand + Channels und identifiziert bekannte Clients */
        public void analyze() {
            detectedClients.clear();
            suspicious = false;
            suspicionReason = "";

            String brandLower = brand.toLowerCase();

            // 1. Brand-Check gegen Hacked Clients
            for (Map.Entry<String, String> entry : KNOWN_HACKED_BRANDS.entrySet()) {
                if (brandLower.contains(entry.getKey())) {
                    suspicious = true;
                    if (!detectedClients.contains(entry.getValue())) {
                        detectedClients.add(entry.getValue());
                    }
                    suspicionReason = "Client-Brand: " + brand;
                }
            }

            // 2. Channel-Check gegen Hacked Clients
            for (String ch : channels) {
                String chLower = ch.toLowerCase();
                for (Map.Entry<String, String> entry : KNOWN_HACKED_CHANNELS.entrySet()) {
                    if (chLower.contains(entry.getKey())) {
                        suspicious = true;
                        if (!detectedClients.contains(entry.getValue())) {
                            detectedClients.add(entry.getValue());
                        }
                        suspicionReason = suspicionReason.isEmpty()
                            ? "Channel: " + ch
                            : suspicionReason + ", Channel: " + ch;
                    }
                }
            }

            // 3. Fabric ohne bekannten legit Mod = verdächtig (viele Hacked Clients basieren auf Fabric)
            if (brandLower.contains("fabric") && detectedClients.isEmpty()) {
                // Prüfen ob es ein bekannter Fabric-basierter legit Client ist
                boolean knownLegit = false;
                for (String legit : KNOWN_LEGIT_BRANDS.keySet()) {
                    if (brandLower.contains(legit)) {
                        knownLegit = true;
                        break;
                    }
                }
                if (!knownLegit) {
                    suspicious = true;
                    suspicionReason = "Fabric-Client (möglicher Hacked Client)";
                    detectedClients.add("§e⚠ Fabric (verdächtig)");
                }
            }
        }

        /** Gibt den formatierten Client-Typ zurück */
        public String getClientDisplayName() {
            if (!brandReceived) return "§7Warte auf Daten...";

            String brandLower = brand.toLowerCase();

            // Bekannte legit Clients
            for (Map.Entry<String, String> entry : KNOWN_LEGIT_BRANDS.entrySet()) {
                if (brandLower.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }

            // Hacked Client erkannt
            if (!detectedClients.isEmpty()) {
                return detectedClients.get(0);
            }

            // Forge
            if (brandLower.contains("forge") || brandLower.contains("fml")) {
                return "§6Forge";
            }

            // Fabric (wenn hier = verdächtig)
            if (brandLower.contains("fabric")) {
                return "§eFabric";
            }

            return "§7" + brand;
        }
    }
}
