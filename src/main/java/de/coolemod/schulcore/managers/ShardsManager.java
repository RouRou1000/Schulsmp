package de.coolemod.schulcore.managers;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.storage.DataManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet PvP-Währung: Shards
 */
public class ShardsManager {
    private final SchulCorePlugin plugin;
    private final DataManager data;
    private final Map<UUID, Integer> shards = new HashMap<>();

    public ShardsManager(SchulCorePlugin plugin) {
        this.plugin = plugin;
        this.data = new DataManager(plugin.getDataFolder(), "shards.yml");
        migrateFromOldFile();
        load();
    }

    /**
     * Migriert alte Daten aus data.yml nach shards.yml (einmalig)
     */
    private void migrateFromOldFile() {
        if (!data.getConfig().contains("shards")) {
            java.io.File oldFile = new java.io.File(plugin.getDataFolder(), plugin.getConfig().getString("data-file", "data.yml"));
            if (oldFile.exists()) {
                org.bukkit.configuration.file.FileConfiguration oldCfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(oldFile);
                if (oldCfg.contains("shards")) {
                    for (String key : oldCfg.getConfigurationSection("shards").getKeys(false)) {
                        data.getConfig().set("shards." + key, oldCfg.getInt("shards." + key));
                    }
                    data.save();
                    plugin.getLogger().info("[ShardsManager] Daten aus data.yml nach shards.yml migriert.");
                }
            }
        }
    }

    public int getShards(UUID uuid) {
        return shards.getOrDefault(uuid, 0);
    }

    public void addShards(UUID uuid, int amount) {
        shards.put(uuid, getShards(uuid) + amount);
        save();
    }

    public boolean removeShards(UUID uuid, int amount) {
        int cur = getShards(uuid);
        if (cur < amount) return false;
        shards.put(uuid, cur - amount);
        save();
        return true;
    }

    public void save() {
        FileConfiguration cfg = data.getConfig();
        for (Map.Entry<UUID, Integer> e : shards.entrySet()) {
            cfg.set("shards." + e.getKey().toString(), e.getValue());
        }
        data.save();
    }

    private void load() {
        FileConfiguration cfg = data.getConfig();
        if (cfg.contains("shards")) {
            for (String key : cfg.getConfigurationSection("shards").getKeys(false)) {
                shards.put(UUID.fromString(key), cfg.getInt("shards." + key));
            }
        }
    }
}
