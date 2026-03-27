package de.coolemod.schulcore.managers;

import de.coolemod.schulcore.SchulCorePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Verwaltet das Wipen und Wiederherstellen von Spielerdaten.
 * Backups werden in plugins/SchulCore/wipes/<UUID>.yml gespeichert.
 */
public class WipeManager {

    private final SchulCorePlugin plugin;
    private final File wipesFolder;

    public WipeManager(SchulCorePlugin plugin) {
        this.plugin = plugin;
        this.wipesFolder = new File(plugin.getDataFolder(), "wipes");
        if (!wipesFolder.exists()) {
            wipesFolder.mkdirs();
        }
    }

    /**
     * Wiped einen Spieler: Backup erstellen, dann alle Daten löschen.
     * @return true wenn erfolgreich
     */
    public boolean wipePlayer(UUID uuid, String playerName, String wipedBy) {
        // Backup erstellen
        if (!createBackup(uuid, playerName, wipedBy)) {
            return false;
        }

        // Daten wipen
        clearPlayerData(uuid);
        return true;
    }

    /**
     * Stellt die Daten eines gewipten Spielers wieder her.
     * @return true wenn erfolgreich
     */
    public boolean unwipePlayer(UUID uuid) {
        File backupFile = getBackupFile(uuid);
        if (!backupFile.exists()) {
            return false;
        }

        FileConfiguration backup = YamlConfiguration.loadConfiguration(backupFile);
        restorePlayerData(uuid, backup);

        // Backup-Datei löschen nach Wiederherstellung
        backupFile.delete();
        return true;
    }

    /**
     * Prüft ob ein Backup für einen Spieler existiert.
     */
    public boolean hasBackup(UUID uuid) {
        return getBackupFile(uuid).exists();
    }

    /**
     * Gibt den Namen des Spielers aus dem Backup zurück.
     */
    public String getBackupPlayerName(UUID uuid) {
        File backupFile = getBackupFile(uuid);
        if (!backupFile.exists()) return null;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(backupFile);
        return cfg.getString("playerName");
    }

    private File getBackupFile(UUID uuid) {
        return new File(wipesFolder, uuid.toString() + ".yml");
    }

    // ==========================================
    //  Backup erstellen
    // ==========================================

    private boolean createBackup(UUID uuid, String playerName, String wipedBy) {
        File backupFile = getBackupFile(uuid);
        FileConfiguration backup = new YamlConfiguration();

        backup.set("playerName", playerName);
        backup.set("wipedAt", System.currentTimeMillis());
        backup.set("wipedBy", wipedBy);

        // Money
        double money = plugin.getEconomy().getBalance(uuid);
        backup.set("money", money);

        // Shards
        int shards = plugin.getShards().getShards(uuid);
        backup.set("shards", shards);

        // Stats
        int kills = plugin.getStats().getKills(uuid);
        int deaths = plugin.getStats().getDeaths(uuid);
        backup.set("kills", kills);
        backup.set("deaths", deaths);

        // Rank
        RankManager.Rank rank = plugin.getRankManager().getRank(uuid);
        backup.set("rank", rank.name());

        // Homes
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            // Homes vom HomeManager
            Map<String, Location> homes = plugin.getHomeManager().getHomesMap(onlinePlayer);
            for (Map.Entry<String, Location> entry : homes.entrySet()) {
                String homeName = entry.getKey();
                Location loc = entry.getValue();
                backup.set("homes." + homeName + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
                backup.set("homes." + homeName + ".x", loc.getX());
                backup.set("homes." + homeName + ".y", loc.getY());
                backup.set("homes." + homeName + ".z", loc.getZ());
                backup.set("homes." + homeName + ".yaw", (double) loc.getYaw());
                backup.set("homes." + homeName + ".pitch", (double) loc.getPitch());
            }
        }

        // Inventar (nur online Spieler)
        if (onlinePlayer != null) {
            // Inventar
            ItemStack[] contents = onlinePlayer.getInventory().getContents();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] != null) {
                    backup.set("inventory." + i, contents[i]);
                }
            }

            // Armor
            ItemStack[] armor = onlinePlayer.getInventory().getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null) {
                    backup.set("armor." + i, armor[i]);
                }
            }

            // Offhand
            ItemStack offhand = onlinePlayer.getInventory().getItemInOffHand();
            if (offhand.getType() != org.bukkit.Material.AIR) {
                backup.set("offhand", offhand);
            }

            // Enderchest
            ItemStack[] enderChest = onlinePlayer.getEnderChest().getContents();
            for (int i = 0; i < enderChest.length; i++) {
                if (enderChest[i] != null) {
                    backup.set("enderchest." + i, enderChest[i]);
                }
            }

            // XP
            backup.set("xpLevel", onlinePlayer.getLevel());
            backup.set("xpProgress", (double) onlinePlayer.getExp());
        }

        try {
            backup.save(backupFile);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("[WipeManager] Fehler beim Speichern des Backups für " + playerName + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ==========================================
    //  Daten wipen
    // ==========================================

    private void clearPlayerData(UUID uuid) {
        // Money auf Default setzen
        double defaultMoney = plugin.getConfig().getDouble("settings.default-money", 0.0);
        plugin.getEconomy().set(uuid, defaultMoney);

        // Shards auf 0
        // ShardsManager hat kein set(), also removeShards bis 0
        int currentShards = plugin.getShards().getShards(uuid);
        if (currentShards > 0) {
            plugin.getShards().removeShards(uuid, currentShards);
        }

        // Stats resetten (kills/deaths via DataManager)
        resetStats(uuid);

        // Rank auf SPIELER
        plugin.getRankManager().setRank(uuid, RankManager.Rank.SPIELER);

        // Homes löschen
        clearHomes(uuid);

        // Online Spieler: Inventar, Enderchest, XP
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            onlinePlayer.getInventory().clear();
            onlinePlayer.getInventory().setArmorContents(new ItemStack[4]);
            onlinePlayer.getInventory().setItemInOffHand(new ItemStack(org.bukkit.Material.AIR));
            onlinePlayer.getEnderChest().clear();
            onlinePlayer.setLevel(0);
            onlinePlayer.setExp(0f);
            onlinePlayer.setHealth(onlinePlayer.getMaxHealth());
            onlinePlayer.setFoodLevel(20);
            onlinePlayer.setSaturation(20f);
            onlinePlayer.updateInventory();
        }
    }

    private void resetStats(UUID uuid) {
        // PlayerStatsManager speichert in data.yml unter stats.kills.<UUID> und stats.deaths.<UUID>
        // Wir müssen die Werte direkt in der DataManager-Config setzen
        try {
            File dataFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("data-file", "data.yml"));
            if (dataFile.exists()) {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
                cfg.set("stats.kills." + uuid.toString(), null);
                cfg.set("stats.deaths." + uuid.toString(), null);
                cfg.save(dataFile);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[WipeManager] Fehler beim Resetten der Stats: " + e.getMessage());
        }
    }

    private void clearHomes(UUID uuid) {
        // Homes werden in homes.yml gespeichert unter <UUID>.<homeName>
        try {
            File homesFile = new File(plugin.getDataFolder(), "homes.yml");
            if (homesFile.exists()) {
                FileConfiguration cfg = YamlConfiguration.loadConfiguration(homesFile);
                cfg.set(uuid.toString(), null);
                cfg.save(homesFile);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[WipeManager] Fehler beim Löschen der Homes: " + e.getMessage());
        }
    }

    // ==========================================
    //  Daten wiederherstellen
    // ==========================================

    private void restorePlayerData(UUID uuid, FileConfiguration backup) {
        // Money
        if (backup.contains("money")) {
            plugin.getEconomy().set(uuid, backup.getDouble("money"));
        }

        // Shards
        if (backup.contains("shards")) {
            int targetShards = backup.getInt("shards");
            int currentShards = plugin.getShards().getShards(uuid);
            int diff = targetShards - currentShards;
            if (diff > 0) {
                plugin.getShards().addShards(uuid, diff);
            } else if (diff < 0) {
                plugin.getShards().removeShards(uuid, -diff);
            }
        }

        // Stats
        if (backup.contains("kills") || backup.contains("deaths")) {
            restoreStats(uuid, backup.getInt("kills", 0), backup.getInt("deaths", 0));
        }

        // Rank
        if (backup.contains("rank")) {
            try {
                RankManager.Rank rank = RankManager.Rank.valueOf(backup.getString("rank"));
                plugin.getRankManager().setRank(uuid, rank);
            } catch (IllegalArgumentException ignored) {}
        }

        // Homes
        if (backup.contains("homes")) {
            restoreHomes(uuid, backup);
        }

        // Online Spieler: Inventar, Enderchest, XP
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            // Inventar
            if (backup.contains("inventory")) {
                onlinePlayer.getInventory().clear();
                for (String key : backup.getConfigurationSection("inventory").getKeys(false)) {
                    int slot = Integer.parseInt(key);
                    ItemStack item = backup.getItemStack("inventory." + key);
                    if (item != null) {
                        onlinePlayer.getInventory().setItem(slot, item);
                    }
                }
            }

            // Armor
            if (backup.contains("armor")) {
                ItemStack[] armorContents = new ItemStack[4];
                for (String key : backup.getConfigurationSection("armor").getKeys(false)) {
                    int slot = Integer.parseInt(key);
                    armorContents[slot] = backup.getItemStack("armor." + key);
                }
                onlinePlayer.getInventory().setArmorContents(armorContents);
            }

            // Offhand
            if (backup.contains("offhand")) {
                ItemStack offhand = backup.getItemStack("offhand");
                if (offhand != null) {
                    onlinePlayer.getInventory().setItemInOffHand(offhand);
                }
            }

            // Enderchest
            if (backup.contains("enderchest")) {
                onlinePlayer.getEnderChest().clear();
                for (String key : backup.getConfigurationSection("enderchest").getKeys(false)) {
                    int slot = Integer.parseInt(key);
                    ItemStack item = backup.getItemStack("enderchest." + key);
                    if (item != null) {
                        onlinePlayer.getEnderChest().setItem(slot, item);
                    }
                }
            }

            // XP
            if (backup.contains("xpLevel")) {
                onlinePlayer.setLevel(backup.getInt("xpLevel"));
                onlinePlayer.setExp((float) backup.getDouble("xpProgress", 0.0));
            }

            onlinePlayer.updateInventory();
        }
    }

    private void restoreStats(UUID uuid, int kills, int deaths) {
        try {
            File dataFile = new File(plugin.getDataFolder(), plugin.getConfig().getString("data-file", "data.yml"));
            FileConfiguration cfg;
            if (dataFile.exists()) {
                cfg = YamlConfiguration.loadConfiguration(dataFile);
            } else {
                cfg = new YamlConfiguration();
            }
            cfg.set("stats.kills." + uuid.toString(), kills);
            cfg.set("stats.deaths." + uuid.toString(), deaths);
            cfg.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[WipeManager] Fehler beim Wiederherstellen der Stats: " + e.getMessage());
        }
    }

    private void restoreHomes(UUID uuid, FileConfiguration backup) {
        try {
            File homesFile = new File(plugin.getDataFolder(), "homes.yml");
            FileConfiguration cfg;
            if (homesFile.exists()) {
                cfg = YamlConfiguration.loadConfiguration(homesFile);
            } else {
                cfg = new YamlConfiguration();
            }

            // Alte Homes für den Spieler entfernen
            cfg.set(uuid.toString(), null);

            // Homes aus Backup wiederherstellen
            if (backup.contains("homes")) {
                for (String homeName : backup.getConfigurationSection("homes").getKeys(false)) {
                    String path = uuid.toString() + "." + homeName;
                    cfg.set(path + ".world", backup.getString("homes." + homeName + ".world"));
                    cfg.set(path + ".x", backup.getDouble("homes." + homeName + ".x"));
                    cfg.set(path + ".y", backup.getDouble("homes." + homeName + ".y"));
                    cfg.set(path + ".z", backup.getDouble("homes." + homeName + ".z"));
                    cfg.set(path + ".yaw", backup.getDouble("homes." + homeName + ".yaw"));
                    cfg.set(path + ".pitch", backup.getDouble("homes." + homeName + ".pitch"));
                }
            }

            cfg.save(homesFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[WipeManager] Fehler beim Wiederherstellen der Homes: " + e.getMessage());
        }
    }
}
