package de.coolemod.donut.systems;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HomeManager implements Listener {
    private final DonutPlugin plugin;
    private final CombatManager combatManager;
    private final Map<UUID, Map<String, Location>> homes = new HashMap<>();
    private final Map<UUID, TeleportTask> pendingTeleports = new HashMap<>();
    private final File homesFile;
    private FileConfiguration homesConfig;
    
    private static final int TELEPORT_DELAY = 5; // seconds
    private static final double MAX_MOVE_DISTANCE = 0.5; // blocks
    
    public HomeManager(DonutPlugin plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.homesFile = new File(plugin.getDataFolder(), "homes.yml");
        loadHomes();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void setHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        homes.computeIfAbsent(uuid, k -> new HashMap<>()).put(name.toLowerCase(), player.getLocation());
        saveHomes();
        player.sendMessage("§8┃ §6§lHOME §8┃ §aHome §f" + name + " §awurde gesetzt!");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }
    
    public void deleteHome(Player player, String name) {
        UUID uuid = player.getUniqueId();
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes != null && playerHomes.remove(name.toLowerCase()) != null) {
            saveHomes();
            player.sendMessage("§8┃ §6§lHOME §8┃ §cHome §f" + name + " §cwurde gelöscht!");
        } else {
            player.sendMessage("§8┃ §6§lHOME §8┃ §cHome §f" + name + " §cexistiert nicht!");
        }
    }
    
    public void teleportHome(Player player, String name) {
        if (combatManager.isInCombat(player)) {
            player.sendMessage("§8┃ §6§lHOME §8┃ §cDu bist im Kampf!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            player.sendMessage("§8┃ §6§lHOME §8┃ §cTeleport bereits aktiv!");
            return;
        }
        
        UUID uuid = player.getUniqueId();
        Map<String, Location> playerHomes = homes.get(uuid);
        if (playerHomes == null || !playerHomes.containsKey(name.toLowerCase())) {
            player.sendMessage("§8┃ §6§lHOME §8┃ §cHome §f" + name + " §cexistiert nicht!");
            return;
        }
        
        Location target = playerHomes.get(name.toLowerCase());
        startTeleport(player, target, "§f" + name);
    }
    
    public void startTeleport(Player player, Location target, String targetName) {
        Location startLoc = player.getLocation().clone();
        
        player.sendMessage("");
        player.sendMessage("§8┃ §6§lTELEPORT §8┃ §7Teleport zu " + targetName);
        player.sendMessage("§8┃ §6§lTELEPORT §8┃ §eBewege dich nicht für §f" + TELEPORT_DELAY + "s");
        player.sendMessage("");
        
        TeleportTask task = new TeleportTask(player, target, startLoc, targetName);
        pendingTeleports.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 0L, 20L);
    }
    
    public void cancelTeleport(Player player, String reason) {
        TeleportTask task = pendingTeleports.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            player.sendMessage("§8┃ §6§lTELEPORT §8┃ §cAbgebrochen: §7" + reason);
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }
    
    public boolean hasPendingTeleport(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }
    
    public List<String> getHomeNames(Player player) {
        Map<String, Location> playerHomes = homes.get(player.getUniqueId());
        if (playerHomes == null) return Collections.emptyList();
        return new ArrayList<>(playerHomes.keySet());
    }
    
    public Map<String, Location> getHomesMap(Player player) {
        Map<String, Location> playerHomes = homes.get(player.getUniqueId());
        if (playerHomes == null) return Collections.emptyMap();
        return new HashMap<>(playerHomes);
    }
    
    public int getHomeCount(Player player) {
        Map<String, Location> playerHomes = homes.get(player.getUniqueId());
        return playerHomes == null ? 0 : playerHomes.size();
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        Player player = e.getPlayer();
        TeleportTask task = pendingTeleports.get(player.getUniqueId());
        if (task == null) return;
        
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        
        // Allow head rotation
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }
        
        // Check if moved too far from start location
        double distance = task.startLocation.distance(to);
        if (distance > MAX_MOVE_DISTANCE) {
            cancelTeleport(player, "Du hast dich bewegt!");
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            e.setCancelled(true); // Cancel damage during teleport
        }
    }
    
    private void loadHomes() {
        if (!homesFile.exists()) {
            homesConfig = new YamlConfiguration();
            return;
        }
        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
        
        for (String uuidStr : homesConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Location> playerHomes = new HashMap<>();
                
                if (homesConfig.isConfigurationSection(uuidStr)) {
                    for (String homeName : homesConfig.getConfigurationSection(uuidStr).getKeys(false)) {
                        String path = uuidStr + "." + homeName;
                        String world = homesConfig.getString(path + ".world");
                        double x = homesConfig.getDouble(path + ".x");
                        double y = homesConfig.getDouble(path + ".y");
                        double z = homesConfig.getDouble(path + ".z");
                        float yaw = (float) homesConfig.getDouble(path + ".yaw");
                        float pitch = (float) homesConfig.getDouble(path + ".pitch");
                        
                        if (Bukkit.getWorld(world) != null) {
                            playerHomes.put(homeName, new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch));
                        }
                    }
                }
                
                if (!playerHomes.isEmpty()) {
                    homes.put(uuid, playerHomes);
                }
            } catch (IllegalArgumentException ignored) {}
        }
    }
    
    private void saveHomes() {
        homesConfig = new YamlConfiguration();
        
        for (Map.Entry<UUID, Map<String, Location>> entry : homes.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<String, Location> home : entry.getValue().entrySet()) {
                String path = uuid + "." + home.getKey();
                Location loc = home.getValue();
                homesConfig.set(path + ".world", loc.getWorld().getName());
                homesConfig.set(path + ".x", loc.getX());
                homesConfig.set(path + ".y", loc.getY());
                homesConfig.set(path + ".z", loc.getZ());
                homesConfig.set(path + ".yaw", loc.getYaw());
                homesConfig.set(path + ".pitch", loc.getPitch());
            }
        }
        
        try {
            homesConfig.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save homes: " + e.getMessage());
        }
    }
    
    private class TeleportTask extends BukkitRunnable {
        private final Player player;
        private final Location target;
        private final Location startLocation;
        private final String targetName;
        private int countdown = TELEPORT_DELAY;
        
        public TeleportTask(Player player, Location target, Location startLocation, String targetName) {
            this.player = player;
            this.target = target;
            this.startLocation = startLocation;
            this.targetName = targetName;
        }
        
        @Override
        public void run() {
            if (!player.isOnline()) {
                pendingTeleports.remove(player.getUniqueId());
                cancel();
                return;
            }
            
            if (combatManager.isInCombat(player)) {
                cancelTeleport(player, "Du wurdest angegriffen!");
                return;
            }
            
            if (countdown <= 0) {
                // Teleport!
                player.teleport(target);
                player.sendMessage("§8┃ §6§lTELEPORT §8┃ §aTeleportiert zu " + targetName);
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                pendingTeleports.remove(player.getUniqueId());
                cancel();
                return;
            }
            
            // Show countdown with progress bar
            String bar = createCountdownBar(countdown, TELEPORT_DELAY);
            String color = countdown <= 2 ? "§a" : countdown <= 3 ? "§e" : "§6";
            player.sendTitle(color + "§l" + countdown, "§8" + bar, 0, 25, 5);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1f + (0.2f * (TELEPORT_DELAY - countdown)));
            
            countdown--;
        }
        
        private String createCountdownBar(int current, int max) {
            StringBuilder sb = new StringBuilder();
            for (int i = max; i > 0; i--) {
                if (i <= current) sb.append("§a●");
                else sb.append("§7○");
                if (i > 1) sb.append(" ");
            }
            return sb.toString();
        }
    }
}
