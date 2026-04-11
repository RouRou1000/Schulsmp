package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.TutorialBook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.List;
import java.util.UUID;

/**
 * Initialisiert Scoreboard, Tab-Name und Chat-Format bei Join.
 * Sendet Fake-OP-Level für F3+F4 Gamemode-Wechsel an berechtigte Spieler.
 * Öffnet Tutorial-Buch und teleportiert neue Spieler zum Spawn.
 */
public class PlayerJoinListener implements Listener {
    private final DonutPlugin plugin;

    public PlayerJoinListener(DonutPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.ensureAutoOp(p);
        boolean firstJoin = !p.hasPlayedBefore();

        // Neue Spieler zum Spawn teleportieren
        if (firstJoin) {
            World world = Bukkit.getWorld("world");
            if (world != null) {
                Location spawn = world.getSpawnLocation();
                spawn.setYaw(-90f);
                spawn.setPitch(0f);
                p.teleport(spawn);
            }
        }

        // Scoreboard etwas verzögern damit LuckPerms Daten geladen hat
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) {
                try {
                    // Neuen Spielern automatisch den "spieler" Rang geben
                    plugin.runLuckPermsJoinSetup(p);

                    var scoreboardManager = plugin.getScoreboardManager();
                    if (scoreboardManager != null) {
                        scoreboardManager.applyFor(p);
                        // Tab-Name setzen
                        p.setPlayerListName(scoreboardManager.getTabName(p));
                        // Auch für alle anderen Spieler aktualisieren
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            online.setPlayerListName(scoreboardManager.getTabName(online));
                        }
                    }

                    // F3+F4 Gamemode-Wechsel für berechtigte Spieler
                    if (p.hasPermission("minecraft.command.gamemode")) {
                        sendFakeOpLevel(p);
                    }
                } catch (Throwable throwable) {
                    plugin.getLogger().warning("Fehler im Join-Setup für " + p.getName() + ": " + throwable.getMessage());
                }
            }
        }, 5L);

        // Tutorial-Buch einmalig öffnen, auch für Spieler die es wegen eines Bugs noch nie gesehen haben.
        if (!hasSeenTutorial(p.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!p.isOnline()) {
                    return;
                }
                TutorialBook.open(plugin, p);
                markTutorialSeen(p.getUniqueId());
            }, 40L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Bei Quit nichts besonderes nötig
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!e.isBedSpawn() && !e.isAnchorSpawn()) {
            World spawn = Bukkit.getWorld("world");
            if (spawn != null) {
                e.setRespawnLocation(spawn.getSpawnLocation().add(0.5, 0, 0.5));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent e) {
        if (e.isCancelled()) return;
        Player p = e.getPlayer();
        String formatted = plugin.getScoreboardManager() != null
            ? plugin.getScoreboardManager().getChatFormat(p)
            : "§7" + p.getName();
        formatted = formatted.replace("%", "%%");
        e.setFormat(formatted + "§8: §f%2$s");
    }

    private void sendFakeOpLevel(Player p) {
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) return;
        try {
            Class<?> protocolLibraryClass = Class.forName("com.comphenix.protocol.ProtocolLibrary");
            Object protocolManager = protocolLibraryClass.getMethod("getProtocolManager").invoke(null);
            Class<?> packetTypeClass = Class.forName("com.comphenix.protocol.PacketType");
            Object entityStatusPacket = Class.forName("com.comphenix.protocol.PacketType$Play$Server")
                .getField("ENTITY_STATUS")
                .get(null);
            Object packet = protocolManager.getClass()
                .getMethod("createPacket", packetTypeClass)
                .invoke(protocolManager, entityStatusPacket);

            Object integers = packet.getClass().getMethod("getIntegers").invoke(packet);
            integers.getClass().getMethod("write", int.class, Object.class)
                .invoke(integers, 0, Integer.valueOf(p.getEntityId()));

            Object bytes = packet.getClass().getMethod("getBytes").invoke(packet);
            bytes.getClass().getMethod("write", int.class, Object.class)
                .invoke(bytes, 0, Byte.valueOf((byte) 28));

            protocolManager.getClass()
                .getMethod("sendServerPacket", Player.class, packet.getClass())
                .invoke(protocolManager, p, packet);
        } catch (Throwable ignored) {}
    }

    private boolean hasSeenTutorial(UUID uuid) {
        List<String> seenPlayers = plugin.getConfig().getStringList("tutorial.seen-players");
        return seenPlayers.contains(uuid.toString());
    }

    private void markTutorialSeen(UUID uuid) {
        List<String> seenPlayers = plugin.getConfig().getStringList("tutorial.seen-players");
        String uuidString = uuid.toString();
        if (seenPlayers.contains(uuidString)) {
            return;
        }
        seenPlayers.add(uuidString);
        plugin.getConfig().set("tutorial.seen-players", seenPlayers);
        plugin.saveConfig();
    }
}
