package de.coolemod.donut.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.metadata.MetadataValue;

/**
 * Schützt temporäre Sign-GUI-Blöcke vor dem Abbauen durch andere Spieler.
 */
public class SignProtectionListener implements Listener {

    private static final String[] SIGN_BLOCK_KEYS = {
        "order_sign_block",
        "worth_sign_block",
        "home_sign_block",
        "ah_sign_block"
    };

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location breakLoc = event.getBlock().getLocation();

        for (Player online : Bukkit.getOnlinePlayers()) {
            for (String key : SIGN_BLOCK_KEYS) {
                if (!online.hasMetadata(key)) continue;
                Location signLoc = online.getMetadata(key).stream()
                    .map(MetadataValue::value)
                    .filter(Location.class::isInstance)
                    .map(Location.class::cast)
                    .findFirst()
                    .orElse(null);
                if (signLoc != null && signLoc.getBlockX() == breakLoc.getBlockX()
                        && signLoc.getBlockY() == breakLoc.getBlockY()
                        && signLoc.getBlockZ() == breakLoc.getBlockZ()
                        && signLoc.getWorld().equals(breakLoc.getWorld())) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage("§cDieses Schild wird gerade verwendet!");
                    return;
                }
            }
        }
    }
}
