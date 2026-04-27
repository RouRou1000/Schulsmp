package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataType;

import java.util.Random;

public class FrostListener implements Listener {

    private final DonutPlugin plugin;
    private final Random random = new Random();

    public FrostListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player attacker)) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon == null || !weapon.hasItemMeta()) return;

        String toolName = weapon.getType().name();
        boolean validWeapon = toolName.endsWith("_SWORD") || toolName.endsWith("_AXE") || toolName.equals("MACE");
        if (!validWeapon) return;

        NamespacedKey frostKey = new NamespacedKey(plugin, "donut_frost");
        var pdc = weapon.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(frostKey, PersistentDataType.INTEGER)) return;

        int level = pdc.get(frostKey, PersistentDataType.INTEGER);

        double chance;
        int durationTicks;
        int amplifier; // 0-based (0 = Slowness I)

        if (level == 1) {
            chance = 0.20;
            durationTicks = 2 * 20; // 2 seconds
            amplifier = 1; // Slowness II
        } else {
            chance = 0.50;
            durationTicks = 3 * 20; // 3 seconds
            amplifier = 2; // Slowness III
        }

        if (random.nextDouble() < chance) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, amplifier, false, true, true));
        }
    }
}
