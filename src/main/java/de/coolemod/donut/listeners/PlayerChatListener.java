package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

/**
 * Sign-basierte Eingabe für Worth-Suche
 */
public class PlayerChatListener implements Listener {
    private final DonutPlugin plugin;

    private static final String WORTH_SIGN_MODE = "worth_sign_mode";
    private static final String WORTH_SIGN_BLOCK = "worth_sign_block";
    private static final String WORTH_SIGN_ORIGINAL = "worth_sign_original";

    public PlayerChatListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    public static void openWorthSearchSign(DonutPlugin plugin, Player player) {
        try {
            cleanupSign(plugin, player);

            org.bukkit.block.Block block = player.getLocation().clone().add(0, 3, 0).getBlock();
            Material originalType = block.getType();

            player.setMetadata(WORTH_SIGN_MODE, new FixedMetadataValue(plugin, "search"));
            player.setMetadata(WORTH_SIGN_BLOCK, new FixedMetadataValue(plugin, block.getLocation()));
            player.setMetadata(WORTH_SIGN_ORIGINAL, new FixedMetadataValue(plugin, originalType.name()));

            block.setType(Material.OAK_SIGN);
            Sign sign = (Sign) block.getState();
            sign.setLine(0, "");
            sign.setLine(1, "^^^^^^^^^^^^^^");
            sign.setLine(2, "Item-Name");
            sign.setLine(3, "eingeben");
            sign.update(false, false);

            player.openSign(sign);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.hasMetadata(WORTH_SIGN_MODE)) {
                    cleanupSign(plugin, player);
                }
            }, 200L);

        } catch (Exception ex) {
            player.sendMessage("§cFehler beim Öffnen der Eingabe!");
        }
    }

    @EventHandler
    public void onSign(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata(WORTH_SIGN_MODE)) return;

        event.setCancelled(true);
        cleanupSign(plugin, player);

        String input = event.getLine(0);
        String trimmed = (input == null) ? "" : input.trim();

        if (trimmed.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                new de.coolemod.donut.gui.WorthGUI(plugin).open(player, 0);
                player.sendMessage("§8┃ §e§lWORTH §8┃ §cSuche abgebrochen.");
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            de.coolemod.donut.gui.WorthGUI.setSearchQuery(player.getUniqueId(), trimmed);
            new de.coolemod.donut.gui.WorthGUI(plugin).open(player, 0);
            player.sendMessage("§8┃ §e§lWORTH §8┃ §7Suche: §f\"" + trimmed + "\"");
        });
    }

    private static void cleanupSign(DonutPlugin plugin, Player player) {
        Location location = player.getMetadata(WORTH_SIGN_BLOCK).stream()
            .map(MetadataValue::value)
            .filter(Location.class::isInstance)
            .map(Location.class::cast)
            .findFirst()
            .orElse(null);
        String originalType = player.getMetadata(WORTH_SIGN_ORIGINAL).stream()
            .map(MetadataValue::asString)
            .findFirst()
            .orElse(null);
        if (location != null && originalType != null) {
            Material material = Material.matchMaterial(originalType);
            if (material != null) {
                location.getBlock().setType(material);
            }
        }
        player.removeMetadata(WORTH_SIGN_MODE, plugin);
        player.removeMetadata(WORTH_SIGN_BLOCK, plugin);
        player.removeMetadata(WORTH_SIGN_ORIGINAL, plugin);
    }
}
