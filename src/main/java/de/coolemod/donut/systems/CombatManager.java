package de.coolemod.donut.systems;

import de.coolemod.donut.DonutPlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager implements Listener {
    private final DonutPlugin plugin;
    private final Map<UUID, Long> combatEndTimes = new HashMap<>();
    private static final int COMBAT_DURATION = 30; // seconds
    
    public CombatManager(DonutPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startCombatTimer();
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player attacker)) return;
        
        // Both players enter combat
        enterCombat(attacker);
        enterCombat(victim);
        
        attacker.sendMessage("§8┃ §c§lKAMPF §8┃ §7Du bist nun im Kampfmodus!");
        victim.sendMessage("§8┃ §c§lKAMPF §8┃ §7Du bist nun im Kampfmodus!");
    }
    
    public void enterCombat(Player player) {
        combatEndTimes.put(player.getUniqueId(), System.currentTimeMillis() + (COMBAT_DURATION * 1000L));
    }
    
    public boolean isInCombat(Player player) {
        Long endTime = combatEndTimes.get(player.getUniqueId());
        if (endTime == null) return false;
        if (System.currentTimeMillis() >= endTime) {
            combatEndTimes.remove(player.getUniqueId());
            return false;
        }
        return true;
    }
    
    public int getRemainingCombatTime(Player player) {
        Long endTime = combatEndTimes.get(player.getUniqueId());
        if (endTime == null) return 0;
        long remaining = endTime - System.currentTimeMillis();
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }
    
    public void leaveCombat(Player player) {
        combatEndTimes.remove(player.getUniqueId());
    }
    
    private void startCombatTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isInCombat(player)) {
                        int remaining = getRemainingCombatTime(player);
                        String bar = createProgressBar(remaining, COMBAT_DURATION);
                        String color = remaining <= 5 ? "§a" : remaining <= 15 ? "§e" : "§c";
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                            new TextComponent("§c§l⚔ KAMPF §8" + bar + " " + color + remaining + "s"));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
    private String createProgressBar(int current, int max) {
        int bars = 20;
        int filled = (int) ((double) current / max * bars);
        StringBuilder sb = new StringBuilder("§8[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) sb.append("§c█");
            else sb.append("§7░");
        }
        sb.append("§8]");
        return sb.toString();
    }
}
