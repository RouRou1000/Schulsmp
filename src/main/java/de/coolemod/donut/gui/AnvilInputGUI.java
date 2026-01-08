package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sign-GUI für Preis-Eingabe
 */
public class AnvilInputGUI implements Listener {
    private final DonutPlugin plugin;
    private static final Map<UUID, Long> awaitingInput = new HashMap<>();

    public AnvilInputGUI(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    public void openPriceInput(Player p) {
        Bukkit.getLogger().info("Opening sign input for " + p.getName());
        
        try {
            // Erstelle temporäres Schild an Spieler-Position
            Block block = p.getLocation().add(0, 3, 0).getBlock();
            Block originalBlock = block;
            Material originalType = block.getType();
            
            // Setze Schild
            block.setType(Material.OAK_SIGN);
            Sign sign = (Sign) block.getState();
            sign.setLine(0, "");
            sign.setLine(1, "^^^^^^^^^^^^^^");
            sign.setLine(2, "Preis eingeben");
            sign.setLine(3, "");
            sign.update(false, false);
            
            // Markiere Spieler als wartend
            awaitingInput.put(p.getUniqueId(), System.currentTimeMillis());
            
            // Öffne Schild für Bearbeitung
            p.openSign(sign);
            
            // Entferne Schild nach 10 Sekunden falls nicht bearbeitet
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (awaitingInput.containsKey(p.getUniqueId())) {
                    awaitingInput.remove(p.getUniqueId());
                    p.removeMetadata("ah_setting_price", plugin);
                    block.setType(originalType);
                    new AuctionCreateGUI(plugin).open(p);
                }
            }, 200L);
            
            Bukkit.getLogger().info("Sign opened successfully");
        } catch (Exception ex) {
            Bukkit.getLogger().severe("Error opening sign: " + ex.getMessage());
            ex.printStackTrace();
            p.sendMessage("§cFehler beim Öffnen der Preis-Eingabe!");
            new AuctionCreateGUI(plugin).open(p);
        }
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();
        
        if (!awaitingInput.containsKey(p.getUniqueId())) {
            return;
        }
        
        e.setCancelled(true);
        awaitingInput.remove(p.getUniqueId());
        
        // Entferne Metadata-Flag
        p.removeMetadata("ah_setting_price", plugin);
        
        // Entferne Schild
        Block block = e.getBlock();
        Bukkit.getScheduler().runTask(plugin, () -> {
            block.setType(Material.AIR);
        });
        
        // Parse Preis von erster Zeile
        String input = e.getLine(0);
        if (input == null || input.trim().isEmpty()) {
            p.sendMessage("§cKeine Eingabe!");
            Bukkit.getScheduler().runTask(plugin, () -> {
                new AuctionCreateGUI(plugin).open(p);
            });
            return;
        }
        
        try {
            double price = Double.parseDouble(input.trim());
            if (price <= 0) {
                p.sendMessage("§cPreis muss positiv sein!");
            } else {
                AuctionCreateGUI.setPrice(p, price);
                p.sendMessage("§a✓ Preis gesetzt: §e$" + "%.2f".formatted(price));
            }
        } catch (NumberFormatException ex) {
            p.sendMessage("§cNur Zahlen erlaubt!");
        }
        
        // Öffne GUI wieder
        Bukkit.getScheduler().runTask(plugin, () -> {
            new AuctionCreateGUI(plugin).open(p);
        });
    }
}

