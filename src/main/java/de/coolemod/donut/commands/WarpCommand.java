package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.RtpGUI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /rtp - Öffnet GUI zur Weltwahl, dann Random Teleport mit Countdown
 */
public class WarpCommand implements CommandExecutor, Listener {
    private final DonutPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> teleporting = ConcurrentHashMap.newKeySet();

    private static final int RTP_RADIUS = 5000;
    private static final int RTP_MIN = 100;
    private static final int COUNTDOWN_SECONDS = 5;
    private static final int COOLDOWN_SECONDS = 10;

    public WarpCommand(DonutPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        // Combat-Check
        if (plugin.getCombatManager() != null && plugin.getCombatManager().isInCombat(p)) {
            p.sendMessage("§8[§a§lRTP§8] §cDu bist im Kampf! Warte bis der Combat-Timer abgelaufen ist.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        if (teleporting.contains(p.getUniqueId())) {
            p.sendMessage("§8[§a§lRTP§8] §cTeleport läuft bereits...");
            return true;
        }

        new RtpGUI().open(p);
        return true;
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof RtpGUI)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        e.setCancelled(true);

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getView().getTopInventory().getSize()) return;

        String worldName;
        if (slot == 11) {
            worldName = "world_farm";
        } else if (slot == 13) {
            worldName = "world_nether";
        } else if (slot == 15) {
            p.sendMessage("§8[§a§lRTP§8] §cDas End ist noch nicht freigeschalten!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        } else {
            return;
        }

        p.closeInventory();
        startRtp(p, worldName);
    }

    private void startRtp(Player p, String worldName) {
        // Combat-Check
        if (plugin.getCombatManager() != null && plugin.getCombatManager().isInCombat(p)) {
            p.sendMessage("§8[§a§lRTP§8] §cDu bist im Kampf! Warte bis der Combat-Timer abgelaufen ist.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Cooldown-Check
        long now = System.currentTimeMillis();
        Long lastUse = cooldowns.get(p.getUniqueId());
        if (lastUse != null) {
            long remaining = COOLDOWN_SECONDS - ((now - lastUse) / 1000);
            if (remaining > 0) {
                p.sendMessage("§8[§a§lRTP§8] §cBitte warte noch §e" + remaining + "s§c.");
                return;
            }
        }

        if (teleporting.contains(p.getUniqueId())) {
            p.sendMessage("§8[§a§lRTP§8] §cTeleport läuft bereits...");
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            p.sendMessage("§8[§a§lRTP§8] §cWelt nicht gefunden!");
            return;
        }

        p.sendMessage("§8[§a§lRTP§8] §7Suche sichere Position...");
        Location safeLoc = findSafeLocation(world);
        if (safeLoc == null) {
            p.sendMessage("§8[§a§lRTP§8] §cKeine sichere Position gefunden. Versuche es erneut!");
            return;
        }

        teleporting.add(p.getUniqueId());
        p.sendMessage("§8[§a§lRTP§8] §7Teleport in §e" + COUNTDOWN_SECONDS + " Sekunden§7... Nicht bewegen!");

        new BukkitRunnable() {
            int count = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (!p.isOnline() || !teleporting.contains(p.getUniqueId())) {
                    cancel();
                    return;
                }

                if (count <= 0) {
                    p.teleport(safeLoc);
                    p.sendMessage("§8[§a§lRTP§8] §aTeleportiert nach §e" + world.getName() + "§a!");
                    p.sendMessage("§8  ▸ §7Position: §f" + safeLoc.getBlockX() + "§8, §f" + safeLoc.getBlockY() + "§8, §f" + safeLoc.getBlockZ());
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    p.spawnParticle(Particle.PORTAL, p.getLocation(), 50, 0.5, 1, 0.5);
                    teleporting.remove(p.getUniqueId());
                    cooldowns.put(p.getUniqueId(), System.currentTimeMillis());
                    cancel();
                    return;
                }

                String color;
                if (count >= 4) color = "§a";
                else if (count >= 2) color = "§e";
                else color = "§c";

                p.sendTitle(color + "§l" + count, "§7Nicht bewegen!", 0, 25, 5);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1f + (0.2f * (COUNTDOWN_SECONDS - count)));

                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!teleporting.contains(e.getPlayer().getUniqueId())) return;

        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;

        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            teleporting.remove(e.getPlayer().getUniqueId());
            e.getPlayer().sendMessage("§8[§a§lRTP§8] §cTeleport abgebrochen! Du hast dich bewegt.");
            e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            e.getPlayer().sendTitle("§c§lAbgebrochen!", "§7Du hast dich bewegt", 0, 30, 10);
        }
    }

    private Location findSafeLocation(World world) {
        for (int i = 0; i < 20; i++) {
            int x = random.nextInt(RTP_RADIUS * 2) - RTP_RADIUS;
            int z = random.nextInt(RTP_RADIUS * 2) - RTP_RADIUS;
            if (Math.abs(x) < RTP_MIN && Math.abs(z) < RTP_MIN) continue;

            int y;
            if (world.getEnvironment() == World.Environment.NETHER) {
                y = findNetherSafeY(world, x, z);
            } else {
                y = world.getHighestBlockYAt(x, z) + 1;
            }

            if (y < 1 || y > 300) continue;

            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (isSafeLocation(loc)) return loc;
        }
        return null;
    }

    private int findNetherSafeY(World world, int x, int z) {
        for (int y = 100; y > 30; y--) {
            Location check = new Location(world, x, y, z);
            if (check.getBlock().getType().isAir()
                && check.clone().add(0, 1, 0).getBlock().getType().isAir()
                && check.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                return y;
            }
        }
        return -1;
    }

    private boolean isSafeLocation(Location loc) {
        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block ground = loc.clone().add(0, -1, 0).getBlock();
        if (!feet.getType().isAir() || !head.getType().isAir()) return false;
        if (!ground.getType().isSolid()) return false;
        String groundName = ground.getType().name();
        return !groundName.contains("LAVA") && !groundName.contains("WATER") && !groundName.contains("MAGMA");
    }
}
