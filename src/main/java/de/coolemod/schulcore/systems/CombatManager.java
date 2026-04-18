package de.coolemod.schulcore.systems;

import de.coolemod.schulcore.SchulCorePlugin;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager implements Listener {
    private final SchulCorePlugin plugin;
    private final Map<UUID, Long> combatEndTimes = new HashMap<>();
    private static final int COMBAT_DURATION = 30; // seconds
    
    public CombatManager(SchulCorePlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startCombatTimer();
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player victim = (Player) e.getEntity();
        Player attacker = null;
        if (e.getDamager() instanceof Player) {
            attacker = (Player) e.getDamager();
        } else if (e.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null || attacker.equals(victim)) return;
        // Nur Nachricht senden wenn Spieler NOCH NICHT im Combat war
        boolean attackerWasInCombat = isInCombat(attacker);
        boolean victimWasInCombat = isInCombat(victim);
        enterCombat(attacker);
        enterCombat(victim);
        if (!attackerWasInCombat) {
            attacker.sendMessage("§8┃ §c§lKAMPF §8┃ §7Du bist nun im Kampfmodus!");
        }
        if (!victimWasInCombat) {
            victim.sendMessage("§8┃ §c§lKAMPF §8┃ §7Du bist nun im Kampfmodus!");
        }
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
    
    public boolean isInCombat(UUID uuid) {
        Long endTime = combatEndTimes.get(uuid);
        if (endTime == null) return false;
        if (System.currentTimeMillis() >= endTime) {
            combatEndTimes.remove(uuid);
            return false;
        }
        return true;
    }
    
    public void leaveCombat(Player player) {
        combatEndTimes.remove(player.getUniqueId());
    }
    
    /**
     * Combat-Logout: Spieler stirbt und droppt Loot
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (!isInCombat(player)) return;
        
        // Spieler ist im Combat ausgeloggt → Loot droppen
        org.bukkit.Location loc = player.getLocation();
        
        // Inventar droppen
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                player.getWorld().dropItemNaturally(loc, item);
            }
        }
        // Rüstung droppen
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                player.getWorld().dropItemNaturally(loc, item);
            }
        }
        // Offhand droppen
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand.getType() != org.bukkit.Material.AIR) {
            player.getWorld().dropItemNaturally(loc, offhand);
        }
        
        // Inventar leeren
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        
        // Spieler töten
        player.setHealth(0);
        
        // Nachricht an Spieler in der Nähe
        String deathMsg = "§8§m          §r §c§l☠ §4" + player.getName() + " §ckombat-geloggt! §c§l☠ §r§8§m          ";
        for (Player nearby : loc.getWorld().getPlayers()) {
            if (nearby.getLocation().distance(loc) <= 150) {
                nearby.sendMessage(deathMsg);
            }
        }
        
        leaveCombat(player);
    }
    
    private void startCombatTimer() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (isInCombat(player)) {
                        int remaining = getRemainingCombatTime(player);
                        String color = remaining <= 5 ? "§a" : remaining <= 15 ? "§e" : "§c";
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                            new TextComponent("§c§l⚔ KAMPF §8· " + color + remaining + "s §7verbleibend"));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }
    
}
