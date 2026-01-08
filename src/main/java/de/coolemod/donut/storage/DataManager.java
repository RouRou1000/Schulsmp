package de.coolemod.donut.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;

/**
 * Einfacher YAML-Datenmanager für persistente Speicherung
 */
public class DataManager {
    private final File file;
    private FileConfiguration cfg;

    public DataManager(File dataFolder, String filename) {
        file = new File(dataFolder, filename);
        if (!file.exists()) {
            try {
                dataFolder.mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        cfg = YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() { return cfg; }

    public void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
