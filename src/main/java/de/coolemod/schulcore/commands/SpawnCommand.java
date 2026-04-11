package de.coolemod.schulcore.commands;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.systems.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * /spawn - Teleportiert den Spieler zum Spawn (mit 5s Countdown)
 */
public class SpawnCommand implements CommandExecutor, Listener {
    private final SchulCorePlugin plugin;
    private static final int COUNTDOWN_SECONDS = 5;
    private static final int COOLDOWN_SECONDS = 10;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    public SpawnCommand(SchulCorePlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cNur für Spieler!");
            return true;
        }
        Player p = (Player) sender;

        // Bereits pending?
        if (pendingTeleports.containsKey(p.getUniqueId())) {
            p.sendMessage("§8┃ §6§lSPAWN §8┃ §cTeleport läuft bereits!");
            return true;
        }

        // Combat check
        CombatManager cm = plugin.getCombatManager();
        if (cm != null && cm.isInCombat(p)) {
            p.sendMessage("§8┃ §6§lSPAWN §8┃ §cDu bist im Kampf!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Cooldown check
        if (cooldowns.containsKey(p.getUniqueId())) {
            long remaining = (cooldowns.get(p.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                p.sendMessage("§8┃ §6§lSPAWN §8┃ §cBitte warte noch §e" + remaining + "s§c!");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            cooldowns.remove(p.getUniqueId());
        }

        // Spawn-Location
        World mainWorld = Bukkit.getWorlds().get(0);
        Location spawnLoc = mainWorld.getSpawnLocation().add(0.5, 0, 0.5);

        // Cooldown sofort setzen
        cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + (COOLDOWN_SECONDS * 1000L));

        p.sendMessage("§8┃ §6§lSPAWN §8┃ §7Teleport in §e" + COUNTDOWN_SECONDS + "s§7... Nicht bewegen!");

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int remaining = COUNTDOWN_SECONDS;
            @Override
            public void run() {
                if (!p.isOnline()) {
                    cancelPending(p.getUniqueId());
                    return;
                }
                // Combat check
                CombatManager cm = plugin.getCombatManager();
                if (cm != null && cm.isInCombat(p)) {
                    p.sendMessage("§8┃ §6§lSPAWN §8┃ §cAbgebrochen! Du bist im Kampf.");
                    p.sendTitle("§c✖ Abgebrochen", "§7Kampf!", 0, 30, 10);
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    cancelPending(p.getUniqueId());
                    cooldowns.remove(p.getUniqueId());
                    return;
                }

                if (remaining <= 0) {
                    p.teleport(spawnLoc);
                    p.sendMessage("§8┃ §6§lSPAWN §8┃ §aZum Spawn teleportiert!");
                    p.sendTitle("§a✔ Spawn", "§7Willkommen zurück!", 0, 40, 10);
                    try {
                        p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, p.getLocation().add(0, 1, 0), 50);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    } catch (Throwable ignored) {}
                    cancelPending(p.getUniqueId());
                    return;
                }

                String color = remaining <= 2 ? "§c" : remaining <= 3 ? "§e" : "§a";
                p.sendTitle(color + "§l" + remaining, "§7Nicht bewegen!", 0, 25, 0);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, remaining <= 2 ? 1.5f : 1f);
                remaining--;
            }
        }, 0L, 20L);

        pendingTeleports.put(p.getUniqueId(), task);
        return true;
    }

    private void cancelPending(UUID uid) {
        BukkitTask task = pendingTeleports.remove(uid);
        if (task != null) task.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent e) {
        UUID uid = e.getPlayer().getUniqueId();
        if (!pendingTeleports.containsKey(uid)) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            cancelPending(uid);
            cooldowns.remove(uid);
            Player p = e.getPlayer();
            p.sendMessage("§8┃ §6§lSPAWN §8┃ §cAbgebrochen! Du hast dich bewegt.");
            p.sendTitle("§c✖ Abgebrochen", "§7Bewegt!", 0, 30, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }
}
