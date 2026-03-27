package de.coolemod.schulcore.spawner;

import de.coolemod.schulcore.SchulCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Verwaltet das gesamte Spawner-System:
 * - Spawner-Items erstellen
 * - Stacking von Spawnern
 * - Drop-Generierung über Zeit
 * - Speichern/Laden von platzierten Spawnern
 */
public class SpawnerSystemManager {
    private final SchulCorePlugin plugin;
    private final Map<Location, PlacedSpawner> placedSpawners = new HashMap<>();
    private final File dataFile;
    private FileConfiguration dataConfig;
    
    // Keys für NBT-Daten
    private final NamespacedKey spawnerTypeKey;
    private final NamespacedKey spawnerStackKey;
    
    // Config
    private int ticksPerDrop = 300; // 15 Sekunden default
    private int maxStackSize = 1000;
    private int baseDropCap = 1000;
    private int capPerStack = 500;

    public SpawnerSystemManager(SchulCorePlugin plugin) {
        this.plugin = plugin;
        this.spawnerTypeKey = new NamespacedKey(plugin, "spawner_type");
        this.spawnerStackKey = new NamespacedKey(plugin, "spawner_stack");
        this.dataFile = new File(plugin.getDataFolder(), "spawners.yml");
        
        loadConfig();
        loadData();
        startDropTask();
    }

    private void loadConfig() {
        ticksPerDrop = plugin.getConfig().getInt("spawner.ticks-per-drop", 300);
        maxStackSize = plugin.getConfig().getInt("spawner.max-stack-size", 1000);
        baseDropCap = plugin.getConfig().getInt("spawner.base-drop-cap", 1000);
        capPerStack = plugin.getConfig().getInt("spawner.cap-per-stack", 500);
        
        // Setze die Cap-Konfiguration für PlacedSpawner
        PlacedSpawner.setCapConfig(baseDropCap, capPerStack);
    }

    private void loadData() {
        if (!dataFile.exists()) {
            dataConfig = new YamlConfiguration();
            return;
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        
        if (dataConfig.contains("spawners")) {
            for (String key : dataConfig.getConfigurationSection("spawners").getKeys(false)) {
                try {
                    String path = "spawners." + key;
                    String world = dataConfig.getString(path + ".world");
                    int x = dataConfig.getInt(path + ".x");
                    int y = dataConfig.getInt(path + ".y");
                    int z = dataConfig.getInt(path + ".z");
                    String type = dataConfig.getString(path + ".type");
                    int stack = dataConfig.getInt(path + ".stack", 1);
                    UUID owner = UUID.fromString(dataConfig.getString(path + ".owner"));
                    long lastDrop = dataConfig.getLong(path + ".lastDrop", System.currentTimeMillis());
                    
                    if (Bukkit.getWorld(world) != null) {
                        Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                        SpawnerType spawnerType = SpawnerType.fromName(type);
                        if (spawnerType != null) {
                            placedSpawners.put(loc, new PlacedSpawner(loc, spawnerType, stack, owner, lastDrop));
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("[SpawnerSystem] Fehler beim Laden: " + key);
                }
            }
        }
        plugin.getLogger().info("[SpawnerSystem] " + placedSpawners.size() + " Spawner geladen.");
    }

    public void saveData() {
        dataConfig = new YamlConfiguration();
        int i = 0;
        for (Map.Entry<Location, PlacedSpawner> entry : placedSpawners.entrySet()) {
            Location loc = entry.getKey();
            PlacedSpawner ps = entry.getValue();
            String path = "spawners." + i;
            dataConfig.set(path + ".world", loc.getWorld().getName());
            dataConfig.set(path + ".x", loc.getBlockX());
            dataConfig.set(path + ".y", loc.getBlockY());
            dataConfig.set(path + ".z", loc.getBlockZ());
            dataConfig.set(path + ".type", ps.getType().name());
            dataConfig.set(path + ".stack", ps.getStackSize());
            dataConfig.set(path + ".owner", ps.getOwner().toString());
            dataConfig.set(path + ".lastDrop", ps.getLastDropTime());
            i++;
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("[SpawnerSystem] Fehler beim Speichern: " + e.getMessage());
        }
    }

    /**
     * Startet den Task, der alle Spawner regelmäßig Drops generieren lässt
     */
    private void startDropTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long interval = (ticksPerDrop / 20) * 1000L; // Ticks zu Millisekunden
            
            for (PlacedSpawner ps : placedSpawners.values()) {
                if (now - ps.getLastDropTime() >= interval) {
                    // Generiere Drops
                    List<ItemStack> drops = ps.getType().generateDrops(ps.getStackSize());
                    ps.addDrops(drops);
                    ps.setLastDropTime(now);
                }
            }
        }, ticksPerDrop, ticksPerDrop);
    }

    /**
     * Erstellt ein Spawner-Item mit Typ und Stack-Größe
     */
    public ItemStack createSpawnerItem(SpawnerType type, int stackSize) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(type.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────");
        lore.add("§7Typ§8: §e" + type.name());
        if (stackSize > 1) {
            lore.add("§7Stack§8: §a" + stackSize + "x");
        }
        lore.add("§8────────────────");
        lore.add("§7Platziere um Drops zu sammeln");
        lore.add("§7§oKeine echten Mobs - nur Items!");
        lore.add("§8────────────────");
        lore.add("§e⚡ Shift+Rechtsklick zum Stacken");
        meta.setLore(lore);
        
        // NBT-Daten speichern
        meta.getPersistentDataContainer().set(spawnerTypeKey, PersistentDataType.STRING, type.name());
        meta.getPersistentDataContainer().set(spawnerStackKey, PersistentDataType.INTEGER, stackSize);
        
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Erstellt ein Spawner-Item mit Stack=1
     */
    public ItemStack createSpawnerItem(SpawnerType type) {
        return createSpawnerItem(type, 1);
    }

    /**
     * Erstellt ein Spawner-Item aus EntityType
     */
    public ItemStack createSpawnerItem(EntityType entityType) {
        SpawnerType type = SpawnerType.fromEntityType(entityType);
        if (type == null) return null;
        return createSpawnerItem(type, 1);
    }

    /**
     * Liest den SpawnerType aus einem Item
     */
    public SpawnerType getSpawnerType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String type = item.getItemMeta().getPersistentDataContainer().get(spawnerTypeKey, PersistentDataType.STRING);
        if (type == null) return null;
        return SpawnerType.fromName(type);
    }

    /**
     * Liest die Stack-Größe aus einem Item
     */
    public int getSpawnerStack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1;
        Integer stack = item.getItemMeta().getPersistentDataContainer().get(spawnerStackKey, PersistentDataType.INTEGER);
        return stack != null ? stack : 1;
    }

    /**
     * Platziert einen Spawner
     */
    public boolean placeSpawner(Location loc, SpawnerType type, int stackSize, UUID owner) {
        if (placedSpawners.containsKey(loc)) return false;
        
        PlacedSpawner ps = new PlacedSpawner(loc, type, stackSize, owner, System.currentTimeMillis());
        placedSpawners.put(loc, ps);
        saveData();
        return true;
    }

    /**
     * Entfernt einen Spawner und gibt ihn als Item zurück
     */
    public ItemStack removeSpawner(Location loc) {
        PlacedSpawner ps = placedSpawners.remove(loc);
        if (ps == null) return null;
        saveData();
        return createSpawnerItem(ps.getType(), ps.getStackSize());
    }

    /**
     * Holt den platzierten Spawner an einer Location
     */
    public PlacedSpawner getPlacedSpawner(Location loc) {
        return placedSpawners.get(loc);
    }

    /**
     * Stackt einen Spawner zu einem existierenden
     * @return true wenn erfolgreich
     */
    public boolean stackSpawner(Location loc, SpawnerType type, int addStack) {
        PlacedSpawner ps = placedSpawners.get(loc);
        if (ps == null) return false;
        if (ps.getType() != type) return false;
        
        long newStack = (long) ps.getStackSize() + addStack;
        if (newStack > maxStackSize) return false;
        
        ps.setStackSize((int) newStack);
        saveData();
        return true;
    }

    /**
     * Gibt alle Spawner eines Spielers zurück
     */
    public List<PlacedSpawner> getSpawnersByOwner(UUID owner) {
        List<PlacedSpawner> result = new ArrayList<>();
        for (PlacedSpawner ps : placedSpawners.values()) {
            if (ps.getOwner().equals(owner)) {
                result.add(ps);
            }
        }
        return result;
    }

    public int getTicksPerDrop() { return ticksPerDrop; }
    public int getMaxStackSize() { return maxStackSize; }
    public NamespacedKey getSpawnerTypeKey() { return spawnerTypeKey; }
    public NamespacedKey getSpawnerStackKey() { return spawnerStackKey; }
}
