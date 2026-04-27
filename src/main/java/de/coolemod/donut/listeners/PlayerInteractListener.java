package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.CrateDetailGUI;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles right-click interactions:
 * - Custom Enchant Books (Drill, Frost): apply enchant to held weapon/tool
 * - Physical chests named after a crate: open the crate
 */
public class PlayerInteractListener implements Listener {
    private final DonutPlugin plugin;

    public PlayerInteractListener(DonutPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack hand = e.getItem();

        // Handle Custom Enchant Book application
        if (hand != null && hand.hasItemMeta()) {
            NamespacedKey enchantTypeKey = new NamespacedKey(plugin, "donut_enchant");
            if (hand.getItemMeta().getPersistentDataContainer().has(enchantTypeKey, PersistentDataType.STRING)) {
                String enchantType = hand.getItemMeta().getPersistentDataContainer().get(enchantTypeKey, PersistentDataType.STRING);
                e.setCancelled(true);
                Player p = e.getPlayer();
                ItemStack offhand = p.getInventory().getItemInOffHand();

                if ("drill".equals(enchantType)) {
                    applyDrill(p, hand, offhand);
                    return;
                }
                if ("frost_1".equals(enchantType) || "frost_2".equals(enchantType)) {
                    applyFrost(p, hand, offhand, enchantType);
                    return;
                }
            }
        }

        // Physical chest named after a crate
        if (e.getClickedBlock() == null) return;
        Block b = e.getClickedBlock();
        if (b.getType() == Material.CHEST && b.getState() instanceof Chest chest) {
            if (chest.getCustomName() != null) {
                e.setCancelled(true);
                String name = chest.getCustomName();
                String found = null;
                for (String id : plugin.getCrateManager().getCrateIds()) {
                    var c = plugin.getCrateManager().getCrate(id);
                    if (c.id.equalsIgnoreCase(name) || c.display.equalsIgnoreCase(name)) { found = id; break; }
                }
                if (found != null) {
                    plugin.getCrateManager().openCrateAnimated(e.getPlayer(), found);
                }
            }
        }
    }

    private void applyDrill(Player p, ItemStack hand, ItemStack offhand) {
        String offhandType = offhand == null ? "" : offhand.getType().name();
        boolean validTool = offhandType.endsWith("_PICKAXE") || offhandType.endsWith("_SHOVEL");
        if (!validTool) {
            p.sendMessage("§8┃ §d§lDRILL §8┃ §cHalte eine Spitzhacke oder Schaufel in der zweiten Hand!");
            return;
        }
        NamespacedKey drillKey = new NamespacedKey(plugin, "donut_drill");
        ItemMeta offMeta = offhand.getItemMeta();
        if (offMeta.getPersistentDataContainer().has(drillKey, PersistentDataType.INTEGER)) {
            p.sendMessage("§8┃ §d§lDRILL §8┃ §cDieses Werkzeug hat den Drill Enchant bereits!");
            return;
        }
        offMeta.getPersistentDataContainer().set(drillKey, PersistentDataType.INTEGER, 1);
        List<String> lore = offMeta.hasLore() ? new ArrayList<>(offMeta.getLore()) : new ArrayList<>();
        lore.add("§5✦ §dDrill §5I");
        offMeta.setLore(lore);
        offhand.setItemMeta(offMeta);
        p.getInventory().setItemInOffHand(offhand);
        consumeBook(p, hand);
        p.sendMessage("§8┃ §d§l✦ DRILL §8┃ §aDer §dDrill §aEnchant wurde erfolgreich angewendet!");
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
    }

    private void applyFrost(Player p, ItemStack hand, ItemStack offhand, String enchantType) {
        String offhandType = offhand == null ? "" : offhand.getType().name();
        boolean validWeapon = offhandType.endsWith("_SWORD") || offhandType.endsWith("_AXE") || offhandType.equals("MACE");
        if (!validWeapon) {
            p.sendMessage("§8┃ §b§lFROST §8┃ §cHalte ein Schwert, eine Axt oder eine Keule in der zweiten Hand!");
            return;
        }
        NamespacedKey frostKey = new NamespacedKey(plugin, "donut_frost");
        ItemMeta offMeta = offhand.getItemMeta();
        if (offMeta.getPersistentDataContainer().has(frostKey, PersistentDataType.INTEGER)) {
            p.sendMessage("§8┃ §b§lFROST §8┃ §cDiese Waffe hat bereits einen Frost Enchant!");
            return;
        }
        int level = enchantType.equals("frost_1") ? 1 : 2;
        String levelRoman = level == 1 ? "I" : "II";
        offMeta.getPersistentDataContainer().set(frostKey, PersistentDataType.INTEGER, level);
        List<String> lore = offMeta.hasLore() ? new ArrayList<>(offMeta.getLore()) : new ArrayList<>();
        lore.add("§b❆ §fFrost §b" + levelRoman);
        offMeta.setLore(lore);
        offhand.setItemMeta(offMeta);
        p.getInventory().setItemInOffHand(offhand);
        consumeBook(p, hand);
        p.sendMessage("§8┃ §b§l❆ FROST §8┃ §aDer §bFrost " + levelRoman + " §aEnchant wurde erfolgreich angewendet!");
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1.4f);
    }

    private void consumeBook(Player p, ItemStack hand) {
        hand.setAmount(hand.getAmount() - 1);
        if (hand.getAmount() <= 0) p.getInventory().setItemInMainHand(null);
        else p.getInventory().setItemInMainHand(hand);
    }
}

