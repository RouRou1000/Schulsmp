package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.storage.DataManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Verwaltet das Geld der Spieler.
 * Geld kann nur über /sell, /pay und Bestellungen verändert werden.
 */
public class EconomyManager {
    private final DonutPlugin plugin;
    private final DataManager data;
    private final Map<UUID, Double> balances = new HashMap<>();

    public EconomyManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.data = new DataManager(plugin.getDataFolder(), plugin.getConfig().getString("data-file", "data.yml"));
        load();
    }

    public double getBalance(UUID uuid) {
        return balances.getOrDefault(uuid, plugin.getConfig().getDouble("settings.default-money", 0.0));
    }

    public void deposit(UUID uuid, double amount) {
        balances.put(uuid, getBalance(uuid) + amount);
        save();
    }

    public boolean withdraw(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current < amount) return false;
        balances.put(uuid, current - amount);
        save();
        return true;
    }

    public void set(UUID uuid, double amount) {
        balances.put(uuid, amount);
        save();
    }

    public void save() {
        FileConfiguration cfg = data.getConfig();
        for (Map.Entry<UUID, Double> e : balances.entrySet()) {
            cfg.set("money." + e.getKey().toString(), e.getValue());
        }
        data.save();
    }

    private void load() {
        FileConfiguration cfg = data.getConfig();
        if (cfg.contains("money")) {
            for (String key : cfg.getConfigurationSection("money").getKeys(false)) {
                balances.put(UUID.fromString(key), cfg.getDouble("money." + key));
            }
        }
    }
}
