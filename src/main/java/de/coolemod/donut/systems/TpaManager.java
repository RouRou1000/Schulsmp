package de.coolemod.donut.systems;

import de.coolemod.donut.DonutPlugin;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TpaManager {
    private final DonutPlugin plugin;
    private final CombatManager combatManager;
    private final HomeManager homeManager;
    private final Map<UUID, TpaRequest> pendingRequests = new HashMap<>();
    
    private static final int REQUEST_TIMEOUT = 60; // seconds
    
    public TpaManager(DonutPlugin plugin, CombatManager combatManager, HomeManager homeManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.homeManager = homeManager;
    }
    
    public void sendTpaRequest(Player sender, Player target) {
        if (sender.equals(target)) {
            sender.sendMessage("§8┃ §b§lTPA §8┃ §cDu kannst dir selbst keine Anfrage senden!");
            return;
        }
        
        if (combatManager.isInCombat(sender)) {
            sender.sendMessage("§8┃ §b§lTPA §8┃ §cDu bist im Kampf!");
            sender.playSound(sender.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        
        // Check if there's already a pending request from this sender
        TpaRequest existing = pendingRequests.get(target.getUniqueId());
        if (existing != null && existing.sender.equals(sender.getUniqueId())) {
            sender.sendMessage("§8┃ §b§lTPA §8┃ §cAnfrage bereits gesendet!");
            return;
        }
        
        // Create new request
        TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId());
        pendingRequests.put(target.getUniqueId(), request);
        
        sender.sendMessage("§8┃ §b§lTPA §8┃ §aAnfrage an §f" + target.getName() + " §agesendet!");
        sender.playSound(sender.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        
        // Send clickable message to target
        target.sendMessage("");
        target.sendMessage("§8┃ §b§lTPA §8┃ §fAnfrage von §e" + sender.getName());
        target.sendMessage("");
        
        // Create clickable buttons
        TextComponent accept = new TextComponent("  §8[§a§l✓ ANNEHMEN§8]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept"));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§aKlicken zum Annehmen")));
        
        TextComponent space = new TextComponent("  ");
        
        TextComponent deny = new TextComponent("§8[§c§l✖ ABLEHNEN§8]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny"));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§cKlicken zum Ablehnen")));
        
        target.spigot().sendMessage(accept, space, deny);
        target.sendMessage("");
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        
        // Timeout task
        new BukkitRunnable() {
            @Override
            public void run() {
                TpaRequest req = pendingRequests.get(target.getUniqueId());
                if (req != null && req.sender.equals(sender.getUniqueId())) {
                    pendingRequests.remove(target.getUniqueId());
                    Player s = Bukkit.getPlayer(sender.getUniqueId());
                    Player t = Bukkit.getPlayer(target.getUniqueId());
                    if (s != null) s.sendMessage("§8┃ §b§lTPA §8┃ §7Anfrage an §f" + target.getName() + " §7abgelaufen");
                    if (t != null) t.sendMessage("§8┃ §b§lTPA §8┃ §7Anfrage von §f" + sender.getName() + " §7abgelaufen");
                }
            }
        }.runTaskLater(plugin, REQUEST_TIMEOUT * 20L);
    }
    
    public void acceptTpa(Player target) {
        TpaRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage("§8┃ §b§lTPA §8┃ §cKeine Anfrage vorhanden!");
            return;
        }
        
        Player sender = Bukkit.getPlayer(request.sender);
        if (sender == null || !sender.isOnline()) {
            target.sendMessage("§8┃ §b§lTPA §8┃ §cSpieler nicht mehr online!");
            return;
        }
        
        if (combatManager.isInCombat(sender)) {
            target.sendMessage("§8┃ §b§lTPA §8┃ §f" + sender.getName() + " §cist im Kampf!");
            sender.sendMessage("§8┃ §b§lTPA §8┃ §cDu bist im Kampf!");
            return;
        }
        
        if (combatManager.isInCombat(target)) {
            target.sendMessage("§8┃ §b§lTPA §8┃ §cDu bist im Kampf!");
            sender.sendMessage("§8┃ §b§lTPA §8┃ §f" + target.getName() + " §cist im Kampf!");
            return;
        }
        
        target.sendMessage("§8┃ §b§lTPA §8┃ §aAnfrage von §f" + sender.getName() + " §aangenommen!");
        sender.sendMessage("§8┃ §b§lTPA §8┃ §f" + target.getName() + " §ahat angenommen!");
        
        // Start teleport with countdown
        homeManager.startTeleport(sender, target.getLocation(), "§f" + target.getName());
    }
    
    public void denyTpa(Player target) {
        TpaRequest request = pendingRequests.remove(target.getUniqueId());
        if (request == null) {
            target.sendMessage("§8┃ §b§lTPA §8┃ §cKeine Anfrage vorhanden!");
            return;
        }
        
        Player sender = Bukkit.getPlayer(request.sender);
        target.sendMessage("§8┃ §b§lTPA §8┃ §cAnfrage abgelehnt!");
        target.playSound(target.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        
        if (sender != null && sender.isOnline()) {
            sender.sendMessage("§8┃ §b§lTPA §8┃ §f" + target.getName() + " §chat abgelehnt!");
            sender.playSound(sender.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }
    
    public boolean hasPendingRequest(Player target) {
        return pendingRequests.containsKey(target.getUniqueId());
    }
    
    private static class TpaRequest {
        final UUID sender;
        final UUID target;
        final long timestamp;
        
        TpaRequest(UUID sender, UUID target) {
            this.sender = sender;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
