package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class AnvilCustomEnchantListener implements Listener {

    private final DonutPlugin plugin;

    public AnvilCustomEnchantListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        AnvilInventory inv = e.getInventory();
        ItemStack base = inv.getItem(0);      // tool (left slot)
        ItemStack addition = inv.getItem(1);  // enchant book (right slot)

        if (base == null || !base.hasItemMeta()) return;
        if (addition == null || !addition.hasItemMeta()) return;

        NamespacedKey enchantTypeKey = new NamespacedKey(plugin, "donut_enchant");
        if (!addition.getItemMeta().getPersistentDataContainer().has(enchantTypeKey, PersistentDataType.STRING)) return;

        String enchantId = addition.getItemMeta().getPersistentDataContainer().get(enchantTypeKey, PersistentDataType.STRING);
        if (enchantId == null) return;

        if ("drill".equals(enchantId)) {
            String toolName = base.getType().name();
            if (!toolName.endsWith("_PICKAXE") && !toolName.endsWith("_SHOVEL")) {
                e.setResult(null);
                return;
            }

            NamespacedKey drillKey = new NamespacedKey(plugin, "donut_drill");
            if (base.getItemMeta().getPersistentDataContainer().has(drillKey, PersistentDataType.INTEGER)) {
                // Already enchanted
                e.setResult(null);
                return;
            }

            ItemStack result = base.clone();
            ItemMeta meta = result.getItemMeta();
            meta.getPersistentDataContainer().set(drillKey, PersistentDataType.INTEGER, 1);
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("§5✦ §dDrill §5I");
            meta.setLore(lore);
            result.setItemMeta(meta);

            e.setResult(result);
            inv.setRepairCost(0);
        }
    }
}
