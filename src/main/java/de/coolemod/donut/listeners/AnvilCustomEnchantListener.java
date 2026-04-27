package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AnvilCustomEnchantListener implements Listener {

    private final DonutPlugin plugin;

    public AnvilCustomEnchantListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    // Shows the result in the output slot
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent e) {
        AnvilInventory inv = e.getInventory();
        ItemStack base = inv.getItem(0);
        ItemStack addition = inv.getItem(1);

        if (base == null || !base.hasItemMeta()) return;
        if (addition == null || !addition.hasItemMeta()) return;

        NamespacedKey enchantTypeKey = new NamespacedKey(plugin, "donut_enchant");
        if (!addition.getItemMeta().getPersistentDataContainer().has(enchantTypeKey, PersistentDataType.STRING)) return;

        String enchantId = addition.getItemMeta().getPersistentDataContainer().get(enchantTypeKey, PersistentDataType.STRING);
        if (enchantId == null) return;

        ItemStack result = buildResult(base, enchantId);
        if (result == null) {
            e.setResult(null);
            return;
        }

        e.setResult(result);
        inv.setRepairCost(0);

        // Force the result slot next tick in case vanilla overrides it
        final ItemStack finalResult = result;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (inv.getItem(0) != null && inv.getItem(1) != null) {
                inv.setItem(2, finalResult);
                inv.setRepairCost(0);
            }
        });
    }

    // Handles the actual click on the result slot — bypasses XP cost entirely
    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilResult(InventoryClickEvent e) {
        if (!(e.getInventory() instanceof AnvilInventory anvilInv)) return;
        if (e.getRawSlot() != 2) return; // output slot only

        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType() == Material.AIR || !result.hasItemMeta()) return;

        NamespacedKey drillKey = new NamespacedKey(plugin, "donut_drill");
        if (!result.getItemMeta().getPersistentDataContainer().has(drillKey, PersistentDataType.INTEGER)) return;

        // Confirm our book is still in slot 1
        ItemStack addition = anvilInv.getItem(1);
        if (addition == null || !addition.hasItemMeta()) return;
        NamespacedKey enchantTypeKey = new NamespacedKey(plugin, "donut_enchant");
        if (!addition.getItemMeta().getPersistentDataContainer().has(enchantTypeKey, PersistentDataType.STRING)) return;

        e.setCancelled(true);

        Player player = (Player) e.getWhoClicked();

        // Clear inputs, no XP consumed
        anvilInv.setItem(0, null);
        anvilInv.setItem(1, null);

        ItemStack give = result.clone();
        @SuppressWarnings("unchecked")
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(give);
        leftover.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

        player.updateInventory();
        player.sendMessage("§5✦ §d§lDrill §r§7wurde erfolgreich angewendet!");
    }

    private ItemStack buildResult(ItemStack base, String enchantId) {
        if ("drill".equals(enchantId)) {
            String toolName = base.getType().name();
            if (!toolName.endsWith("_PICKAXE") && !toolName.endsWith("_SHOVEL")) return null;

            NamespacedKey drillKey = new NamespacedKey(plugin, "donut_drill");
            if (base.getItemMeta().getPersistentDataContainer().has(drillKey, PersistentDataType.INTEGER)) return null;

            ItemStack result = base.clone();
            ItemMeta meta = result.getItemMeta();
            meta.getPersistentDataContainer().set(drillKey, PersistentDataType.INTEGER, 1);
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("§5✦ §dDrill §5I");
            meta.setLore(lore);
            result.setItemMeta(meta);
            return result;
        }
        return null;
    }
}

