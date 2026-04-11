package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /spawn – Teleportiert zum Spawn mit 5-Sekunden Countdown
 * Bewegung bricht den Teleport ab
 */
public class SpawnCommand implements CommandExecutor, Listener {

    private final DonutPlugin plugin;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> teleporting = ConcurrentHashMap.newKeySet();

    private static final int COUNTDOWN_SECONDS = 5;
    private static final int COOLDOWN_SECONDS = 10;

    public SpawnCommand(DonutPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cNur für Spieler.");
            return true;
        }

        // Combat-Check
        if (plugin.getCombatManager() != null && plugin.getCombatManager().isInCombat(p)) {
            p.sendMessage("§8[§a§lSpawn§8] §cDu bist im Kampf! Warte bis der Combat-Timer abgelaufen ist.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Cooldown-Check
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(p.getUniqueId());
        if (lastUse != null) {
            long remaining = COOLDOWN_SECONDS - ((now - lastUse) / 1000);
            if (remaining > 0) {
                p.sendMessage("§8[§a§lSpawn§8] §cBitte warte noch §e" + remaining + "s§c.");
                return true;
            }
        }

        if (teleporting.contains(p.getUniqueId())) {
            p.sendMessage("§8[§a§lSpawn§8] §cTeleport läuft bereits...");
            return true;
        }

        teleporting.add(p.getUniqueId());

        p.sendMessage("§8[§a§lSpawn§8] §7Teleport in §e" + COUNTDOWN_SECONDS + " Sekunden§7... Nicht bewegen!");

        new BukkitRunnable() {
            int count = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (!p.isOnline() || !teleporting.contains(p.getUniqueId())) {
                    cancel();
                    return;
                }

                if (count <= 0) {
                    // Teleport immer zum Spawn der Hauptwelt "world"
                    World mainWorld = Bukkit.getWorld("world");
                    if (mainWorld == null) mainWorld = Bukkit.getWorlds().getFirst();
                    Location spawn = mainWorld.getSpawnLocation().add(0.5, 0, 0.5);
                    p.teleport(spawn);
                    p.sendMessage("§8[§a§lSpawn§8] §aTeleportiert!");
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    p.spawnParticle(Particle.PORTAL, p.getLocation(), 50, 0.5, 1, 0.5);
                    teleporting.remove(p.getUniqueId());
                    cooldowns.put(p.getUniqueId(), System.currentTimeMillis());
                    cancel();
                    return;
                }

                // Countdown-Anzeige
                String color;
                if (count >= 4) color = "§a";
                else if (count >= 2) color = "§e";
                else color = "§c";

                p.sendTitle(color + "§l" + count, "§7Nicht bewegen!", 0, 25, 5);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1f + (0.2f * (COUNTDOWN_SECONDS - count)));

                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);

        return true;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!teleporting.contains(e.getPlayer().getUniqueId())) return;

        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;

        // Nur horizontale Bewegung prüfen (Kopf drehen erlaubt)
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            teleporting.remove(e.getPlayer().getUniqueId());
            e.getPlayer().sendMessage("§8[§a§lSpawn§8] §cTeleport abgebrochen! Du hast dich bewegt.");
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            e.getPlayer().sendTitle("§c§lAbgebrochen!", "§7Du hast dich bewegt", 0, 30, 10);
        }
    }
}
