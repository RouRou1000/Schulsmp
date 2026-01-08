package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.HashSet;
import java.util.Set;

/**
 * Verwaltet Regeln für Spawner
 * - Spawner droppen nur mit Silk Touch
 * - Kaufen mit Shards wird über GUI/Shop erledigt
 */
public class SpawnerManager {
    private final DonutPlugin plugin;
    private final Set<EntityType> allowed = new HashSet<>();

    public SpawnerManager(DonutPlugin plugin) {
        this.plugin = plugin;
        for (String s : plugin.getConfig().getStringList("allowed-spawners")) {
            try {
                allowed.add(EntityType.valueOf(s));
            } catch (Exception ignored) {}
        }
    }

    public boolean isAllowed(EntityType type) { return allowed.contains(type); }

    // Wird vom Listener genutzt: erstellt ItemStack mit Spawner-Blockstate
    public ItemStack createSpawnerItem(EntityType type) {
        ItemStack sp = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) sp.getItemMeta();
        if (meta != null) {
            CreatureSpawner st = (CreatureSpawner) meta.getBlockState();
            st.setSpawnedType(type);
            meta.setBlockState(st);
            meta.setDisplayName("§6Spawner: §e" + type.name());
            sp.setItemMeta(meta);
        }
        return sp;
    }

    // Wenn ohne Silk Touch zerstört, nichts droppen
    public void handleBlockBreak(BlockBreakEvent e) {
        if (!(e.getBlock().getState() instanceof CreatureSpawner)) return;
        ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
        boolean silk = item != null && item.containsEnchantment(org.bukkit.enchantments.Enchantment.SILK_TOUCH);
        if (!silk) {
            e.setDropItems(false);
            // optional: Effekt oder Nachricht
            e.getPlayer().sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cKein Drop ohne Silk Touch.");
        } else {
            // normaler Drop wird über Event gehandhabt; in modernen API evtl manuell drop
        }
    }
}
