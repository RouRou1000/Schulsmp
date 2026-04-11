package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.systems.PlacedSpawner;
import de.coolemod.donut.systems.SpawnerType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class SpawnerManager {
    private final DonutPlugin plugin;
    private final Map<Location, PlacedSpawner> placedSpawners = new HashMap<>();
    private final File dataFile;
    private FileConfiguration dataConfig;

    private final NamespacedKey spawnerTypeKey;
    private final NamespacedKey spawnerStackKey;

    private int ticksPerDrop = 300;
    private int maxStackSize = 1000;
    private int baseDropCap = 1000;
    private int capPerStack = 500;

    public SpawnerManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.spawnerTypeKey = new NamespacedKey(plugin, "spawner_type");
        this.spawnerStackKey = new NamespacedKey(plugin, "spawner_stack");
        this.dataFile = new File(plugin.getDataFolder(), "spawners.yml");
        loadConfig();
        loadData();
        startDropTask();
    }

    private void loadConfig() {
        ticksPerDrop = Math.max(1, plugin.getConfig().getInt("spawner.ticks-per-drop", 300));
        maxStackSize = plugin.getConfig().getInt("spawner.max-stack-size", 1000);
        baseDropCap = plugin.getConfig().getInt("spawner.base-drop-cap", 1000);
        capPerStack = plugin.getConfig().getInt("spawner.cap-per-stack", 500);
        PlacedSpawner.setCapConfig(baseDropCap, capPerStack);
    }

    private void loadData() {
        if (!dataFile.exists()) { dataConfig = new YamlConfiguration(); return; }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.isConfigurationSection("spawners")) {
            var spawnerSection = dataConfig.getConfigurationSection("spawners");
            if (spawnerSection == null) {
                return;
            }
            for (String key : spawnerSection.getKeys(false)) {
                try {
                    String path = "spawners." + key;
                    String world = dataConfig.getString(path + ".world");
                    int x = dataConfig.getInt(path + ".x");
                    int y = dataConfig.getInt(path + ".y");
                    int z = dataConfig.getInt(path + ".z");
                    String type = dataConfig.getString(path + ".type");
                    int stack = dataConfig.getInt(path + ".stack", 1);
                    String ownerValue = dataConfig.getString(path + ".owner");
                    if (ownerValue == null) {
                        continue;
                    }
                    UUID owner = UUID.fromString(ownerValue);
                    long lastDrop = dataConfig.getLong(path + ".lastDrop", System.currentTimeMillis());
                    if (Bukkit.getWorld(world) != null) {
                        Location loc = new Location(Bukkit.getWorld(world), x, y, z);
                        SpawnerType spawnerType = SpawnerType.fromName(type);
                        if (spawnerType != null)
                            placedSpawners.put(loc, new PlacedSpawner(loc, spawnerType, stack, owner, lastDrop));
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
            if (loc == null || loc.getWorld() == null) {
                continue;
            }
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
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("[SpawnerSystem] Fehler beim Speichern: " + e.getMessage()); }
    }

    private void startDropTask() {
        try {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                long now = System.currentTimeMillis();
                long interval = (ticksPerDrop / 20) * 1000L;
                for (PlacedSpawner ps : placedSpawners.values()) {
                    if (now - ps.getLastDropTime() >= interval) {
                        List<ItemStack> drops = ps.getType().generateDrops(ps.getStackSize());
                        ps.addDrops(drops);
                        ps.setLastDropTime(now);
                    }
                }
            }, ticksPerDrop, ticksPerDrop);
        } catch (Throwable throwable) {
            plugin.getLogger().warning("[SpawnerSystem] Drop-Task konnte nicht gestartet werden: " + throwable.getMessage());
        }
    }

    public ItemStack createSpawnerItem(SpawnerType type, int stackSize) {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(type.getDisplayName());
        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────");
        lore.add("§7Typ§8: §e" + type.name());
        lore.add("§8────────────────");
        lore.add("§7Platziere um Drops zu sammeln");
        lore.add("§7§oKeine echten Mobs - nur Items!");
        lore.add("§8────────────────");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(spawnerTypeKey, PersistentDataType.STRING, type.name());
        meta.getPersistentDataContainer().set(spawnerStackKey, PersistentDataType.INTEGER, stackSize);
        meta.setMaxStackSize(64);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSpawnerItem(SpawnerType type) { return createSpawnerItem(type, 1); }

    public ItemStack createSpawnerItem(EntityType entityType) {
        SpawnerType type = SpawnerType.fromEntityType(entityType);
        if (type == null) return null;
        return createSpawnerItem(type, 1);
    }

    public SpawnerType getSpawnerType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String type = item.getItemMeta().getPersistentDataContainer().get(spawnerTypeKey, PersistentDataType.STRING);
        if (type == null) return null;
        return SpawnerType.fromName(type);
    }

    public int getSpawnerStack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 1;
        Integer stack = item.getItemMeta().getPersistentDataContainer().get(spawnerStackKey, PersistentDataType.INTEGER);
        return stack != null ? stack : 1;
    }

    public boolean placeSpawner(Location loc, SpawnerType type, int stackSize, UUID owner) {
        if (placedSpawners.containsKey(loc)) return false;
        placedSpawners.put(loc, new PlacedSpawner(loc, type, stackSize, owner, System.currentTimeMillis()));
        saveData();
        return true;
    }

    public ItemStack removeSpawner(Location loc) {
        PlacedSpawner ps = placedSpawners.remove(loc);
        if (ps == null) return null;
        saveData();
        return createSpawnerItem(ps.getType(), 1);
    }

    public int removeFromStack(Location loc, int amount) {
        PlacedSpawner ps = placedSpawners.get(loc);
        if (ps == null || amount <= 0) return 0;
        int removeAmount = Math.min(amount, ps.getStackSize());
        if (removeAmount >= ps.getStackSize()) {
            placedSpawners.remove(loc);
        } else {
            ps.setStackSize(ps.getStackSize() - removeAmount);
        }
        saveData();
        return removeAmount;
    }

    public List<ItemStack> createSpawnerItemStacks(SpawnerType type, int amount) {
        List<ItemStack> result = new ArrayList<>();
        int remaining = Math.max(0, amount);
        while (remaining > 0) {
            int stackAmount = Math.min(64, remaining);
            ItemStack item = createSpawnerItem(type, 1);
            item.setAmount(stackAmount);
            result.add(item);
            remaining -= stackAmount;
        }
        return result;
    }

    /**
     * Baut 1 Spawner ab (Stack -1). Gibt den Spawner als Item zurück.
     * Entfernt den PlacedSpawner komplett wenn Stack auf 0 fällt.
     * @return das Spawner-Item oder null wenn keiner da
     */
    public ItemStack decreaseStack(Location loc) {
        PlacedSpawner ps = placedSpawners.get(loc);
        if (ps == null) return null;
        if (ps.getStackSize() <= 1) {
            return removeSpawner(loc);
        }
        ps.setStackSize(ps.getStackSize() - 1);
        saveData();
        return createSpawnerItem(ps.getType(), 1);
    }

    public PlacedSpawner getPlacedSpawner(Location loc) { return placedSpawners.get(loc); }

    public List<PlacedSpawner> getSpawnersByOwner(UUID owner) {
        List<PlacedSpawner> result = new ArrayList<>();
        for (PlacedSpawner ps : placedSpawners.values()) if (ps.getOwner().equals(owner)) result.add(ps);
        return result;
    }

    public int getTicksPerDrop() { return ticksPerDrop; }
    public int getMaxStackSize() { return maxStackSize; }
    public NamespacedKey getSpawnerTypeKey() { return spawnerTypeKey; }
    public NamespacedKey getSpawnerStackKey() { return spawnerStackKey; }
}
