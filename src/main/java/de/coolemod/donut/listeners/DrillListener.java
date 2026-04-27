package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the custom "Drill" enchantment.
 * When a player breaks a block with a pickaxe or shovel that has the Drill enchant,
 * it mines a 3×1×3 area (3×3 on the mined face).
 */
public class DrillListener implements Listener {
    private final DonutPlugin plugin;
    private final NamespacedKey drillKey;
    private final Set<UUID> drilling = new HashSet<>();
    private final Random random = new Random();

    public DrillListener(DonutPlugin plugin) {
        this.plugin = plugin;
        this.drillKey = new NamespacedKey(plugin, "donut_drill");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) return;
        if (drilling.contains(player.getUniqueId())) return; // Prevent recursion

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || tool.getType().isAir()) return;
        if (!hasDrillEnchant(tool)) return;

        String typeName = tool.getType().name();
        if (!typeName.endsWith("_PICKAXE") && !typeName.endsWith("_SHOVEL")) return;

        Block center = e.getBlock();
        float pitch = player.getLocation().getPitch();
        float yaw = player.getLocation().getYaw();

        drilling.add(player.getUniqueId());
        try {
            for (Block extra : getAdjacentBlocks(center, pitch, yaw)) {
                if (extra.getType().isAir()) continue;
                if (isUnbreakable(extra)) continue;
                extra.breakNaturally(tool);
                if (!damageTool(player, tool)) break; // Tool broke, stop drilling
            }
        } finally {
            drilling.remove(player.getUniqueId());
        }
    }

    public boolean hasDrillEnchant(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(drillKey, PersistentDataType.INTEGER);
    }

    private boolean isUnbreakable(Block b) {
        Material m = b.getType();
        return m == Material.BEDROCK
            || m == Material.BARRIER
            || m == Material.COMMAND_BLOCK
            || m == Material.CHAIN_COMMAND_BLOCK
            || m == Material.REPEATING_COMMAND_BLOCK
            || m == Material.STRUCTURE_BLOCK
            || m == Material.STRUCTURE_VOID
            || m == Material.END_PORTAL
            || m == Material.END_PORTAL_FRAME
            || m == Material.NETHER_PORTAL
            || m == Material.END_GATEWAY;
    }

    /**
     * Returns the 8 blocks surrounding the center on the mined face (3×3 minus center).
     * Determines face orientation from player pitch/yaw.
     */
    private List<Block> getAdjacentBlocks(Block center, float pitch, float yaw) {
        List<Block> blocks = new ArrayList<>();
        // Normalize yaw to [0, 360)
        yaw = ((yaw % 360) + 360) % 360;

        if (Math.abs(pitch) > 60) {
            // Looking mostly up or down → horizontal face, mine 3×3 on XZ plane
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    blocks.add(center.getRelative(x, 0, z));
                }
            }
        } else if ((yaw >= 315 || yaw < 45) || (yaw >= 135 && yaw < 225)) {
            // Facing mostly south (yaw≈0) or north (yaw≈180) → vertical Z-face, mine 3×3 on XY plane
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    if (x == 0 && y == 0) continue;
                    blocks.add(center.getRelative(x, y, 0));
                }
            }
        } else {
            // Facing mostly east (yaw≈270) or west (yaw≈90) → vertical X-face, mine 3×3 on YZ plane
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (y == 0 && z == 0) continue;
                    blocks.add(center.getRelative(0, y, z));
                }
            }
        }
        return blocks;
    }

    /**
     * Applies 1 durability to the tool respecting Unbreaking enchantment.
     * Returns false if the tool broke.
     */
    private boolean damageTool(Player p, ItemStack tool) {
        if (!(tool.getItemMeta() instanceof Damageable dm)) return true;

        int unbreaking = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        if (unbreaking > 0 && random.nextInt(unbreaking + 1) != 0) return true;

        int newDamage = dm.getDamage() + 1;
        short maxDurability = tool.getType().getMaxDurability();
        if (maxDurability > 0 && newDamage >= maxDurability) {
            p.getInventory().setItemInMainHand(null);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            return false;
        }
        dm.setDamage(newDamage);
        tool.setItemMeta(dm);
        p.getInventory().setItemInMainHand(tool);
        return true;
    }
}
