package de.coolemod.schulcore.listeners;

import de.coolemod.schulcore.SchulCorePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Aktualisiert Kills/Deaths, gibt Shards bei PvP-Kill und zeigt Custom Kill-Messages
 */
public class PlayerDeathListener implements Listener {
    private final SchulCorePlugin plugin;
    private static final double KILL_MSG_RADIUS = 150.0;

    public PlayerDeathListener(SchulCorePlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        plugin.getStats().addDeath(victim.getUniqueId());
        
        // Standard Death-Message unterdrücken
        e.setDeathMessage(null);
        
        if (killer != null) {
            // PvP Kill
            plugin.getStats().addKill(killer.getUniqueId());
            int shards = plugin.getConfig().getInt("settings.shard-per-kill", 10);
            plugin.getShards().addShards(killer.getUniqueId(), shards);
            killer.sendMessage("§8┃ §a§l✓ §a+" + shards + " Shards §7für den Kill!");
            
            // Combat-Status beider Spieler aufheben
            plugin.getCombatManager().leaveCombat(killer);
            plugin.getCombatManager().leaveCombat(victim);
            
            // Custom Kill-Message an Spieler in der Nähe
            String killMsg = "§c☠ §4" + killer.getName() + " §chat §4" + victim.getName() + " §cgetötet!";
            
            for (Player nearby : victim.getWorld().getPlayers()) {
                if (nearby.getLocation().distance(victim.getLocation()) <= KILL_MSG_RADIUS) {
                    nearby.sendMessage(killMsg);
                }
            }
        } else {
            // PvE / Sonstiger Tod
            String deathCause = getDeathCause(victim);
            String deathMsg = "§c☠ §4" + victim.getName() + " " + deathCause;
            
            for (Player nearby : victim.getWorld().getPlayers()) {
                if (nearby.getLocation().distance(victim.getLocation()) <= KILL_MSG_RADIUS) {
                    nearby.sendMessage(deathMsg);
                }
            }
        }
    }
    
    private String getDeathCause(Player victim) {
        if (victim.getLastDamageCause() == null) return "§cist gestorben";
        return switch (victim.getLastDamageCause().getCause()) {
            case FALL -> "§cist in den Tod gestürzt";
            case DROWNING -> "§cist ertrunken";
            case FIRE, FIRE_TICK, LAVA -> "§cist verbrannt";
            case VOID -> "§cist ins Void gefallen";
            case BLOCK_EXPLOSION -> "§cwurde in die Luft gejagt";
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> "§cwurde von einem Mob getötet";
            case PROJECTILE -> "§cwurde erschossen";
            case STARVATION -> "§cist verhungert";
            case POISON -> "§cwurde vergiftet";
            case WITHER -> "§cist verdorrt";
            case FALLING_BLOCK -> "§cwurde zerquetscht";
            case SUFFOCATION -> "§cist erstickt";
            case FREEZE -> "§cist erfroren";
            case LIGHTNING -> "§cwurde vom Blitz getroffen";
            case MAGIC -> "§cist durch Magie gestorben";
            case CRAMMING -> "§cwurde erdrückt";
            default -> "§cist gestorben";
        };
    }
}