package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.storage.DataManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet PvP-Währung: Shards
 */
public class ShardsManager {
    private final DonutPlugin plugin;
    private final DataManager data;
    private final Map<UUID, Integer> shards = new HashMap<>();

    public ShardsManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.data = new DataManager(plugin.getDataFolder(), plugin.getConfig().getString("data-file", "data.yml"));
        load();
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
