package de.coolemod.donut.systems;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HomeListener implements Listener {
    private final DonutPlugin plugin;
    private final HomeManager homeManager;
    private final HomeGUI homeGUI;
    
    public HomeListener(DonutPlugin plugin, HomeManager homeManager, HomeGUI homeGUI) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.homeGUI = homeGUI;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        
        String title = e.getView().getTitle();
        if (!title.contains("ᴅᴇɪɴᴇ ʜᴏᴍᴇs") && !title.contains("ʜᴏᴍᴇ ʟᴏsᴄʜᴇɴ")) return;
        
        e.setCancelled(true);
        
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;
        
        String action = homeGUI.getAction(clicked);
        if (action == null || action.equals("border")) return;
        
        String homeName = homeGUI.getHomeName(clicked);
        
        switch (action) {
            case "home" -> {
                if (e.isLeftClick()) {
                    // Teleport to home
                    player.closeInventory();
                    homeManager.teleportHome(player, homeName);
                } else if (e.isRightClick()) {
                    // Open delete confirmation
                    player.openInventory(homeGUI.createDeleteConfirmGUI(homeName));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                }
            }
            case "create" -> {
                player.closeInventory();
                openHomeNameSignGUI(player);
            }
            case "confirm_delete" -> {
                homeManager.deleteHome(player, homeName);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.openInventory(homeGUI.createHomesGUI(player));
                }, 2L);
            }
            case "cancel_delete" -> {
                player.openInventory(homeGUI.createHomesGUI(player));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            }
            case "close" -> {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
            }
        }
    }
    
    private void openHomeNameSignGUI(Player player) {
        try {
            org.bukkit.block.Block block = player.getLocation().add(0, 3, 0).getBlock();
            Material originalType = block.getType();
            
            // Store original block info in metadata
            player.setMetadata("home_create_mode", new FixedMetadataValue(plugin, true));
            player.setMetadata("home_sign_block", new FixedMetadataValue(plugin, block.getLocation()));
            player.setMetadata("home_sign_original", new FixedMetadataValue(plugin, originalType.name()));
            
            block.setType(Material.OAK_SIGN);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setLine(0, "");
            sign.setLine(1, "^^^^^^^^^^^^^^");
            sign.setLine(2, "Home Name");
            sign.setLine(3, "eingeben");
            sign.update(false, false);
            
            player.openSign(sign);
            
            // Fallback cleanup after 200 ticks
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.hasMetadata("home_sign_block")) {
                    org.bukkit.Location loc = (org.bukkit.Location) player.getMetadata("home_sign_block").get(0).value();
                    String origType = player.getMetadata("home_sign_original").get(0).asString();
                    loc.getBlock().setType(Material.valueOf(origType));
                    player.removeMetadata("home_create_mode", plugin);
                    player.removeMetadata("home_sign_block", plugin);
                    player.removeMetadata("home_sign_original", plugin);
                }
            }, 200L);
            
        } catch (Exception ex) {
            player.sendMessage("§8┃ §6§lHOME §8┃ §cFehler beim Öffnen der Eingabe!");
            player.removeMetadata("home_create_mode", plugin);
            player.removeMetadata("home_sign_block", plugin);
            player.removeMetadata("home_sign_original", plugin);
        }
    }
    
    private void cleanupSign(Player player) {
        if (player.hasMetadata("home_sign_block")) {
            org.bukkit.Location loc = (org.bukkit.Location) player.getMetadata("home_sign_block").get(0).value();
            String origType = player.getMetadata("home_sign_original").get(0).asString();
            loc.getBlock().setType(Material.valueOf(origType));
            player.removeMetadata("home_sign_block", plugin);
            player.removeMetadata("home_sign_original", plugin);
        }
    }
    
    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player player = e.getPlayer();
        
        if (!player.hasMetadata("home_create_mode")) return;
        
        player.removeMetadata("home_create_mode", plugin);
        
        // Immediately cleanup the sign
        cleanupSign(player);
        
        String homeName = e.getLine(0).trim();
        
        if (homeName.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§8┃ §6§lHOME §8┃ §7Erstellung abgebrochen.");
                player.openInventory(homeGUI.createHomesGUI(player));
            }, 2L);
            return;
        }
        
        // Validate home name
        if (homeName.length() > 16) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§8┃ §6§lHOME §8┃ §cName zu lang! §8(§7Max. 16 Zeichen§8)");
                player.openInventory(homeGUI.createHomesGUI(player));
            }, 2L);
            return;
        }
        
        if (!homeName.matches("[a-zA-Z0-9_]+")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§8┃ §6§lHOME §8┃ §cUngültiger Name! §8(§7Nur A-Z, 0-9, _§8)");
                player.openInventory(homeGUI.createHomesGUI(player));
            }, 2L);
            return;
        }
        
        // Check limit
        if (homeManager.getHomeNames(player).size() >= HomeGUI.MAX_HOMES) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§8┃ §6§lHOME §8┃ §cMaximum von §e" + HomeGUI.MAX_HOMES + " §cHomes erreicht!");
                player.openInventory(homeGUI.createHomesGUI(player));
            }, 2L);
            return;
        }
        
        // Check if home already exists
        if (homeManager.getHomeNames(player).contains(homeName.toLowerCase())) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage("§8┃ §6§lHOME §8┃ §cDer Name §f" + homeName + " §cexistiert bereits!");
                player.openInventory(homeGUI.createHomesGUI(player));
            }, 2L);
            return;
        }
        
        // Create home
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            homeManager.setHome(player, homeName);
            player.openInventory(homeGUI.createHomesGUI(player));
        }, 2L);
    }
}
