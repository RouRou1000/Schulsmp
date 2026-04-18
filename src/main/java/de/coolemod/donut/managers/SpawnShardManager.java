package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Vergibt Shards, solange sich Spieler in der konfigurierten Welt aufhalten.
 */
public class SpawnShardManager implements Listener {
    private final DonutPlugin plugin;
    private final Map<UUID, Integer> secondsInSpawn = new HashMap<>();
    private BukkitTask task;

    public SpawnShardManager(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!isEnabled()) {
            return;
        }

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        secondsInSpawn.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        secondsInSpawn.remove(event.getPlayer().getUniqueId());
    }

    private void tick() {
        if (!isEnabled()) {
            secondsInSpawn.clear();
            return;
        }

        int intervalSeconds = getIntervalSeconds();
        int rewardAmount = getRewardAmount();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            if (player.isDead() || !isInRewardWorld(player)) {
                secondsInSpawn.remove(uuid);
                continue;
            }

            int seconds = secondsInSpawn.getOrDefault(uuid, 0) + 1;
            if (seconds >= intervalSeconds) {
                plugin.getShards().addShards(uuid, rewardAmount);
                secondsInSpawn.put(uuid, 0);
                sendRewardActionBar(player, rewardAmount, intervalSeconds);
                continue;
            }

            secondsInSpawn.put(uuid, seconds);
            sendCountdownActionBar(player, intervalSeconds - seconds, rewardAmount);
        }
    }

    private void sendCountdownActionBar(Player player, int remainingSeconds, int rewardAmount) {
        int seconds = Math.max(0, remainingSeconds);
        String color = seconds <= 5 ? "§a" : seconds <= 15 ? "§e" : "§c";
        sendActionBar(player, "§d§l✦ SHARDS " + color + seconds + "s §7bis §d+"
            + NumberFormatter.formatInt(rewardAmount) + " " + shardLabel(rewardAmount));
    }

    private void sendRewardActionBar(Player player, int rewardAmount, int nextIntervalSeconds) {
        sendActionBar(player, "§d§l✦ SHARDS §a+" + NumberFormatter.formatInt(rewardAmount) + " "
            + shardLabel(rewardAmount) + " §8| §7Naechster in §e" + nextIntervalSeconds + "s");
    }

    private void sendActionBar(Player player, String message) {
        if (plugin.getCombatManager() != null && plugin.getCombatManager().isInCombat(player)) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    private boolean isInRewardWorld(Player player) {
        World rewardWorld = Bukkit.getWorld(getRewardWorldName());
        if (rewardWorld == null) {
            if (Bukkit.getWorlds().isEmpty()) {
                return false;
            }
            rewardWorld = Bukkit.getWorlds().getFirst();
        }

        return player.getWorld().equals(rewardWorld);
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("settings.spawn-shards.enabled", true);
    }

    private int getIntervalSeconds() {
        return Math.max(1, plugin.getConfig().getInt("settings.spawn-shards.interval-seconds", 30));
    }

    private int getRewardAmount() {
        return Math.max(1, plugin.getConfig().getInt("settings.spawn-shards.reward", 1));
    }

    private String getRewardWorldName() {
        return plugin.getConfig().getString("settings.spawn-shards.world-name", "world");
    }

    private String shardLabel(int amount) {
        return amount == 1 ? "Shard" : "Shards";
    }
}