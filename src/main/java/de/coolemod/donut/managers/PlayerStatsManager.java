package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.storage.DataManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Speichert Kills, Deaths und berechnet K/D Ratio
 */
public class PlayerStatsManager {
    private final DonutPlugin plugin;
    private final DataManager data;
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();

    public PlayerStatsManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.data = new DataManager(plugin.getDataFolder(), plugin.getConfig().getString("data-file", "data.yml"));
        load();
    }

    public int getKills(UUID uuid) { return kills.getOrDefault(uuid, 0); }
    public int getDeaths(UUID uuid) { return deaths.getOrDefault(uuid, 0); }

    public void addKill(UUID uuid) {
        kills.put(uuid, getKills(uuid) + 1);
        save();
    }

    public void addDeath(UUID uuid) {
        deaths.put(uuid, getDeaths(uuid) + 1);
        save();
    }

    public double getKD(UUID uuid) {
        int d = getDeaths(uuid);
        if (d == 0) return (double) getKills(uuid);
        return (double) getKills(uuid) / d;
    }

    public void save() {
        FileConfiguration cfg = data.getConfig();
        for (Map.Entry<UUID, Integer> e : kills.entrySet()) {
            cfg.set("stats.kills." + e.getKey().toString(), e.getValue());
        }
        for (Map.Entry<UUID, Integer> e : deaths.entrySet()) {
            cfg.set("stats.deaths." + e.getKey().toString(), e.getValue());
        }
        data.save();
    }

    private void load() {
        FileConfiguration cfg = data.getConfig();
        if (cfg.contains("stats.kills")) {
            for (String key : cfg.getConfigurationSection("stats.kills").getKeys(false)) {
                kills.put(UUID.fromString(key), cfg.getInt("stats.kills." + key));
            }
        }
        if (cfg.contains("stats.deaths")) {
            for (String key : cfg.getConfigurationSection("stats.deaths").getKeys(false)) {
                deaths.put(UUID.fromString(key), cfg.getInt("stats.deaths." + key));
            }
        }
    }
}
