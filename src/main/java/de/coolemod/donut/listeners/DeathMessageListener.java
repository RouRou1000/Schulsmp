package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Custom Death Messages im 250-Block-Radius
 */
public class DeathMessageListener implements Listener {
    private static final double RADIUS = 250.0;
    private final DonutPlugin plugin;

    public DeathMessageListener(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        String message = buildDeathMessage(victim);

        // Vanilla-Nachricht unterdrücken
        e.setDeathMessage(null);

        // Custom-Nachricht nur im Radius senden
        Location loc = victim.getLocation();
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.getLocation().distance(loc) <= RADIUS) {
                p.sendMessage(message);
            }
        }
    }

    private String buildDeathMessage(Player victim) {
        String name = victim.getName();
        EntityDamageEvent lastDamage = victim.getLastDamageCause();
        Player killer = victim.getKiller();

        // PvP Kill
        if (killer != null) {
            String killerName = killer.getName();
            String weapon = getWeaponName(killer);
            if (weapon != null) {
                return "§8☠ §c" + name + " §7wurde von §c" + killerName + " §7mit §e" + weapon + " §7getötet.";
            }
            return "§8☠ §c" + name + " §7wurde von §c" + killerName + " §7getötet.";
        }

        if (lastDamage == null) {
            return "§8☠ §c" + name + " §7ist gestorben.";
        }

        // Mob Kill
        if (lastDamage instanceof EntityDamageByEntityEvent entityEvent) {
            Entity damager = entityEvent.getDamager();

            // Projectile (Skeleton, etc.)
            if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
                String mobName = getMobName(shooter);
                return "§8☠ §c" + name + " §7wurde von §6" + mobName + " §7erschossen.";
            }

            String mobName = getMobName(damager);
            return switch (damager.getType()) {
                case CREEPER -> "§8☠ §c" + name + " §7wurde von einem §2Creeper §7in die Luft gejagt.";
                case ZOMBIE, ZOMBIE_VILLAGER, HUSK, DROWNED ->
                    "§8☠ §c" + name + " §7wurde von einem §2" + mobName + " §7gefressen.";
                case SKELETON, STRAY, BOGGED ->
                    "§8☠ §c" + name + " §7wurde von einem §7" + mobName + " §7erschossen.";
                case SPIDER, CAVE_SPIDER ->
                    "§8☠ §c" + name + " §7wurde von einer §4" + mobName + " §7gebissen.";
                case ENDERMAN ->
                    "§8☠ §c" + name + " §7wurde von einem §5Enderman §7vernichtet.";
                case BLAZE ->
                    "§8☠ §c" + name + " §7wurde von einem §6Blaze §7verbrannt.";
                case GHAST ->
                    "§8☠ §c" + name + " §7wurde von einem §fGhast §7abgeschossen.";
                case WITHER_SKELETON ->
                    "§8☠ §c" + name + " §7wurde von einem §8Wither-Skelett §7erschlagen.";
                case WITHER ->
                    "§8☠ §c" + name + " §7wurde vom §8§lWither §7vernichtet.";
                case ENDER_DRAGON ->
                    "§8☠ §c" + name + " §7wurde vom §5§lEnderdrachen §7zermalmt.";
                case PIGLIN_BRUTE ->
                    "§8☠ §c" + name + " §7wurde von einem §6Piglin-Brute §7niedergestreckt.";
                case HOGLIN, ZOGLIN ->
                    "§8☠ §c" + name + " §7wurde von einem §6" + mobName + " §7aufgespießt.";
                case WARDEN ->
                    "§8☠ §c" + name + " §7wurde vom §3§lWarden §7pulverisiert.";
                case GUARDIAN, ELDER_GUARDIAN ->
                    "§8☠ §c" + name + " §7wurde von einem §b" + mobName + " §7gelasert.";
                case PHANTOM ->
                    "§8☠ §c" + name + " §7wurde von einem §9Phantom §7aus dem Himmel gerissen.";
                case VEX ->
                    "§8☠ §c" + name + " §7wurde von einem §7Vex §7erstochen.";
                case RAVAGER ->
                    "§8☠ §c" + name + " §7wurde von einem §8Ravager §7überrannt.";
                case VINDICATOR ->
                    "§8☠ §c" + name + " §7wurde von einem §8Vindicator §7hingerichtet.";
                case EVOKER ->
                    "§8☠ §c" + name + " §7wurde von einem §8Evoker §7verhext.";
                case PILLAGER ->
                    "§8☠ §c" + name + " §7wurde von einem §8Pillager §7erschossen.";
                case WITCH ->
                    "§8☠ §c" + name + " §7wurde von einer §5Hexe §7vergiftet.";
                case SLIME, MAGMA_CUBE ->
                    "§8☠ §c" + name + " §7wurde von einem §a" + mobName + " §7zerquetscht.";
                case SILVERFISH ->
                    "§8☠ §c" + name + " §7wurde von §7Silberfischchen §7zerfressen.";
                case IRON_GOLEM ->
                    "§8☠ §c" + name + " §7wurde von einem §fEisengolem §7zertrümmert.";
                case BEE ->
                    "§8☠ §c" + name + " §7wurde von einer §e Biene §7gestochen.";
                case WOLF ->
                    "§8☠ §c" + name + " §7wurde von einem §fWolf §7zerfleischt.";
                case PIGLIN ->
                    "§8☠ §c" + name + " §7wurde von einem §6Piglin §7attackiert.";
                case SHULKER ->
                    "§8☠ §c" + name + " §7wurde von einem §dShulker §7beschossen.";
                case TNT, TNT_MINECART ->
                    "§8☠ §c" + name + " §7wurde von §cTNT §7in Stücke gesprengt.";
                case BREEZE ->
                    "§8☠ §c" + name + " §7wurde von einem §bBreeze §7weggeblasen.";
                default ->
                    "§8☠ §c" + name + " §7wurde von §6" + mobName + " §7getötet.";
            };
        }

        // Environment deaths
        return switch (lastDamage.getCause()) {
            case FALL ->
                "§8☠ §c" + name + " §7fiel zu tief und starb.";
            case DROWNING ->
                "§8☠ §c" + name + " §7ist ertrunken.";
            case FIRE, FIRE_TICK ->
                "§8☠ §c" + name + " §7ist verbrannt.";
            case LAVA ->
                "§8☠ §c" + name + " §7nahm ein Bad in Lava.";
            case SUFFOCATION ->
                "§8☠ §c" + name + " §7ist in einer Wand erstickt.";
            case STARVATION ->
                "§8☠ §c" + name + " §7ist verhungert.";
            case VOID ->
                "§8☠ §c" + name + " §7fiel aus der Welt.";
            case LIGHTNING ->
                "§8☠ §c" + name + " §7wurde vom Blitz getroffen.";
            case POISON ->
                "§8☠ §c" + name + " §7wurde vergiftet.";
            case MAGIC ->
                "§8☠ §c" + name + " §7wurde von Magie getötet.";
            case WITHER ->
                "§8☠ §c" + name + " §7ist verwelkt.";
            case FALLING_BLOCK ->
                "§8☠ §c" + name + " §7wurde von einem Block erschlagen.";
            case THORNS ->
                "§8☠ §c" + name + " §7starb an Dornen.";
            case DRAGON_BREATH ->
                "§8☠ §c" + name + " §7wurde vom Drachenatem verbrannt.";
            case FLY_INTO_WALL ->
                "§8☠ §c" + name + " §7flog gegen eine Wand.";
            case FREEZE ->
                "§8☠ §c" + name + " §7ist erfroren.";
            case SONIC_BOOM ->
                "§8☠ §c" + name + " §7wurde vom §3Warden §7zerschmettert.";
            case CRAMMING ->
                "§8☠ §c" + name + " §7wurde zu Tode gequetscht.";
            case DRYOUT ->
                "§8☠ §c" + name + " §7ist ausgetrocknet.";
            case CONTACT ->
                "§8☠ §c" + name + " §7wurde von einem Kaktus erstochen.";
            case HOT_FLOOR ->
                "§8☠ §c" + name + " §7ist auf Magmablöcken verbrannt.";
            case CAMPFIRE ->
                "§8☠ §c" + name + " §7ist im Lagerfeuer verbrannt.";
            case ENTITY_EXPLOSION, BLOCK_EXPLOSION ->
                "§8☠ §c" + name + " §7wurde in die Luft gesprengt.";
            default ->
                "§8☠ §c" + name + " §7ist gestorben.";
        };
    }

    private String getWeaponName(Player player) {
        var item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) return null;
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        String name = item.getType().name().replace("_", " ").toLowerCase();
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private String getMobName(Entity entity) {
        if (entity.getCustomName() != null) return entity.getCustomName();
        return switch (entity.getType()) {
            case ZOMBIE -> "Zombie";
            case ZOMBIE_VILLAGER -> "Zombie-Dorfbewohner";
            case HUSK -> "Husk";
            case DROWNED -> "Ertrunkener";
            case SKELETON -> "Skelett";
            case STRAY -> "Stray";
            case BOGGED -> "Bogged";
            case SPIDER -> "Spinne";
            case CAVE_SPIDER -> "Höhlenspinne";
            case CREEPER -> "Creeper";
            case ENDERMAN -> "Enderman";
            case BLAZE -> "Blaze";
            case GHAST -> "Ghast";
            case WITHER_SKELETON -> "Wither-Skelett";
            case WITHER -> "Wither";
            case ENDER_DRAGON -> "Enderdrache";
            case PIGLIN -> "Piglin";
            case PIGLIN_BRUTE -> "Piglin-Brute";
            case HOGLIN -> "Hoglin";
            case ZOGLIN -> "Zoglin";
            case WARDEN -> "Warden";
            case GUARDIAN -> "Wächter";
            case ELDER_GUARDIAN -> "Großer Wächter";
            case PHANTOM -> "Phantom";
            case VEX -> "Vex";
            case RAVAGER -> "Ravager";
            case VINDICATOR -> "Vindicator";
            case EVOKER -> "Evoker";
            case PILLAGER -> "Plünderer";
            case WITCH -> "Hexe";
            case SLIME -> "Schleim";
            case MAGMA_CUBE -> "Magmawürfel";
            case SILVERFISH -> "Silberfischchen";
            case IRON_GOLEM -> "Eisengolem";
            case BEE -> "Biene";
            case WOLF -> "Wolf";
            case SHULKER -> "Shulker";
            case BREEZE -> "Breeze";
            default -> entity.getType().name().replace("_", " ").toLowerCase();
        };
    }
}
