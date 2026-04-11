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
        if (cfg.isConfigurationSection("stats.kills")) {
            var killsSection = cfg.getConfigurationSection("stats.kills");
            if (killsSection != null) {
                for (String key : killsSection.getKeys(false)) {
                    try {
                        kills.put(UUID.fromString(key), cfg.getInt("stats.kills." + key));
                    } catch (IllegalArgumentException exception) {
                        plugin.getLogger().warning("Überspringe ungültigen Kill-Eintrag: " + key);
                    }
                }
            }
        }
        if (cfg.isConfigurationSection("stats.deaths")) {
            var deathsSection = cfg.getConfigurationSection("stats.deaths");
            if (deathsSection != null) {
                for (String key : deathsSection.getKeys(false)) {
                    try {
                        deaths.put(UUID.fromString(key), cfg.getInt("stats.deaths." + key));
                    } catch (IllegalArgumentException exception) {
                        plugin.getLogger().warning("Überspringe ungültigen Death-Eintrag: " + key);
                    }
                }
            }
        }
    }
}
