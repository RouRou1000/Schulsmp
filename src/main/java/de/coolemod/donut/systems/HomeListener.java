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
import org.bukkit.metadata.MetadataValue;

import java.util.Optional;

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
        if (!homeGUI.isHomeInventory(e.getView().getTopInventory())) return;

        e.setCancelled(true);
        if (e.getRawSlot() >= e.getView().getTopInventory().getSize()) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null) return;

        String action = homeGUI.getAction(clicked);
        if (action == null || action.equals("border")) return;

        String homeName = homeGUI.getHomeName(clicked);

        switch (action) {
            case "home" -> {
                if (e.isLeftClick()) {
                    player.closeInventory();
                    homeManager.teleportHome(player, homeName);
                } else if (e.isRightClick()) {
                    player.openInventory(homeGUI.createDeleteConfirmGUI(homeName));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
                }
            }
            case "create" -> {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.7f, 1.2f);
                openHomeNameSignGUI(player);
            }
            case "confirm_delete" -> {
                homeManager.deleteHome(player, homeName);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 0.9f);
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

            block.setType(Material.OAK_SIGN, false);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setWaxed(false);
            org.bukkit.block.sign.SignSide front = sign.getSide(org.bukkit.block.sign.Side.FRONT);
            front.setLine(0, "");
            front.setLine(1, "^^^^^^^^^^^^^^");
            front.setLine(2, "Home Name");
            front.setLine(3, "eingeben");
            sign.update(true, false);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() == Material.OAK_SIGN) {
                    player.openSign((org.bukkit.block.Sign) block.getState(), org.bukkit.block.sign.Side.FRONT);
                }
            }, 3L);

            // Fallback cleanup after 200 ticks
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.hasMetadata("home_sign_block")) {
                    cleanupSign(player);
                    player.removeMetadata("home_create_mode", plugin);
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
        org.bukkit.Location loc = getMetadataLocation(player, "home_sign_block");
        String origType = getMetadataString(player, "home_sign_original");
        if (loc != null && origType != null) {
            Material material = Material.matchMaterial(origType);
            if (material != null) {
                loc.getBlock().setType(material);
            }
        }
        player.removeMetadata("home_sign_block", plugin);
        player.removeMetadata("home_sign_original", plugin);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent e) {
        Player player = e.getPlayer();

        if (!player.hasMetadata("home_create_mode")) return;

        player.removeMetadata("home_create_mode", plugin);

        // Immediately cleanup the sign
        cleanupSign(player);

        String homeName = Optional.ofNullable(e.getLine(0)).orElse("").trim();

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

    private org.bukkit.Location getMetadataLocation(Player player, String key) {
        return player.getMetadata(key).stream()
            .map(MetadataValue::value)
            .filter(org.bukkit.Location.class::isInstance)
            .map(org.bukkit.Location.class::cast)
            .findFirst()
            .orElse(null);
    }

    private String getMetadataString(Player player, String key) {
        return player.getMetadata(key).stream()
            .map(MetadataValue::asString)
            .findFirst()
            .orElse(null);
    }
}
