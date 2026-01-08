package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

/**
 * /rtp <weltname> - Random Teleport in eine beliebige Welt
 */
public class WarpCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    private final Random random = new Random();
    private static final int RTP_RADIUS = 5000; // Max Radius für Random TP
    private static final int RTP_MIN = 100; // Min Distanz vom Spawn

    public WarpCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("Nur Spieler."); return true; }
        Player p = (Player) sender;
        
        if (args.length == 0) {
            p.sendMessage("§6§lRandom Teleport");
            p.sendMessage("§7Verwendung: §e/rtp <weltname>");
            p.sendMessage("§7Verfügbare Welten:");
            for (World w : Bukkit.getWorlds()) {
                String env = getEnvironmentIcon(w.getEnvironment());
                p.sendMessage("§7 - " + env + " §f" + w.getName());
            }
            return true;
        }
        
        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Welt '§f" + worldName + "§c' nicht gefunden!");
            p.sendMessage("§7Verfügbare Welten:");
            for (World w : Bukkit.getWorlds()) {
                p.sendMessage("§7 - §f" + w.getName());
            }
            return true;
        }
        
        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§eSuche sichere Position...");
        
        // Finde sichere Random-Position
        Location safeLoc = findSafeLocation(world);
        if (safeLoc == null) {
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§c✗ Konnte keine sichere Position finden. Versuche es erneut!");
            return true;
        }
        
        p.teleport(safeLoc);
        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§a✓ Teleportiert zu §e" + world.getName() + "§a!");
        p.sendMessage("§7Position: §f" + safeLoc.getBlockX() + "§7, §f" + safeLoc.getBlockY() + "§7, §f" + safeLoc.getBlockZ());
        
        // Effekte
        try {
            p.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, p.getLocation().add(0,1,0), 50);
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        } catch (Throwable ignored) {}
        
        return true;
    }
    
    private Location findSafeLocation(World world) {
        int maxAttempts = 20;
        
        for (int i = 0; i < maxAttempts; i++) {
            int x = random.nextInt(RTP_RADIUS * 2) - RTP_RADIUS;
            int z = random.nextInt(RTP_RADIUS * 2) - RTP_RADIUS;
            
            // Mindestdistanz vom Spawn
            if (Math.abs(x) < RTP_MIN && Math.abs(z) < RTP_MIN) continue;
            
            // Höchsten sicheren Block finden
            int y;
            if (world.getEnvironment() == World.Environment.NETHER) {
                // Nether: Suche freien Raum zwischen 32-100
                y = findNetherSafeY(world, x, z);
            } else {
                y = world.getHighestBlockYAt(x, z) + 1;
            }
            
            if (y < 1 || y > 300) continue;
            
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            
            // Prüfe ob sicher (kein Lava, kein Wasser, Boden vorhanden)
            if (isSafeLocation(loc)) {
                return loc;
            }
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
        org.bukkit.block.Block feet = loc.getBlock();
        org.bukkit.block.Block head = loc.clone().add(0, 1, 0).getBlock();
        org.bukkit.block.Block ground = loc.clone().add(0, -1, 0).getBlock();
        
        // Luft für Spieler
        if (!feet.getType().isAir() || !head.getType().isAir()) return false;
        
        // Fester Boden
        if (!ground.getType().isSolid()) return false;
        
        // Kein Lava/Wasser
        String groundName = ground.getType().name();
        if (groundName.contains("LAVA") || groundName.contains("WATER") || groundName.contains("MAGMA")) return false;
        
        return true;
    }
    
    private String getEnvironmentIcon(World.Environment env) {
        switch (env) {
            case NETHER: return "§c🔥";
            case THE_END: return "§d⭐";
            default: return "§a🌍";
        }
    }
}