package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Speichert AntiCheat-Bannhistorie pro Spieler für automatische Staffeln.
 */
public class AntiCheatHistoryManager {
    private final DonutPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public AntiCheatHistoryManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "anticheat-history.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Konnte anticheat-history.yml nicht erstellen: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte anticheat-history.yml nicht speichern: " + e.getMessage());
        }
    }

    public int getStrikeCount(UUID playerId) {
        return config.getInt(path(playerId, "count"), 0);
    }

    public Duration getNextBanDuration(UUID playerId) {
        return getDurationForStrike(getStrikeCount(playerId) + 1);
    }

    public Duration getDurationForStrike(int strike) {
        return switch (Math.max(strike, 1)) {
            case 1 -> Duration.ofDays(14);
            case 2 -> Duration.ofDays(30);
            case 3 -> Duration.ofDays(60);
            default -> Duration.ofDays(90);
        };
    }

    public int recordBan(UUID playerId, String playerName, String reason, Duration duration, String source) {
        int newCount = getStrikeCount(playerId) + 1;
        config.set(path(playerId, "count"), newCount);
        config.set(path(playerId, "last-name"), playerName);
        config.set(path(playerId, "last-reason"), reason);
        config.set(path(playerId, "last-source"), source);
        config.set(path(playerId, "last-duration-seconds"), duration.getSeconds());
        config.set(path(playerId, "last-ban-at"), Instant.now().toString());
        save();
        return newCount;
    }

    private String path(UUID playerId, String suffix) {
        return "players." + playerId + "." + suffix;
    }
}