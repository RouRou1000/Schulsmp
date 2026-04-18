package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Verwaltet pro-Spieler Einstellungen (TPA, Glow, MobSpawn etc.)
 * Persistiert in plugins/SchulCore/settings.yml
 */
public class SettingsManager {
    private final DonutPlugin plugin;
    private final File file;
    private FileConfiguration config;

    public enum Setting {
        TPA_ENABLED("tpa-enabled", true, "TPA Anfragen"),
        TPAHERE_ENABLED("tpahere-enabled", true, "TPAHere Anfragen"),
        MOB_SPAWN_BLOCK("mob-spawn-block", false, "Mob-Schutz"),
        CLAN_GLOW("clan-glow", false, "Clan Glow"),
        GLOBAL_CHAT("global-chat", true, "Globaler Chat"),
        DEATH_MESSAGES("death-messages", true, "Todesnachrichten"),
        SCOREBOARD("scoreboard", true, "Scoreboard");

        private final String key;
        private final boolean defaultValue;
        private final String displayName;

        Setting(String key, boolean defaultValue, String displayName) {
            this.key = key;
            this.defaultValue = defaultValue;
            this.displayName = displayName;
        }

        public String getKey() { return key; }
        public boolean getDefault() { return defaultValue; }
        public String getDisplayName() { return displayName; }
    }

    public SettingsManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "settings.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Konnte settings.yml nicht erstellen: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte settings.yml nicht speichern: " + e.getMessage());
        }
    }

    public boolean getSetting(UUID playerId, Setting setting) {
        String path = playerId.toString() + "." + setting.getKey();
        return config.getBoolean(path, setting.getDefault());
    }

    public void setSetting(UUID playerId, Setting setting, boolean value) {
        String path = playerId.toString() + "." + setting.getKey();
        config.set(path, value);
        save();
    }

    public void toggleSetting(UUID playerId, Setting setting) {
        boolean current = getSetting(playerId, setting);
        setSetting(playerId, setting, !current);
    }
}
