package de.coolemod.schulcore.commands;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.systems.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

import org.bukkit.scheduler.BukkitTask;

/**
 * /rtp - Öffnet GUI mit Welten-Auswahl (Nether, Farm, End coming soon)
 */
public class WarpCommand implements CommandExecutor, Listener {
    private final SchulCorePlugin plugin;
    private final Random random = new Random();
    private static final int RTP_RADIUS = 5000;
    private static final int RTP_MIN = 100;
    private static final int COOLDOWN_SECONDS = 10;
    private static final int COUNTDOWN_SECONDS = 5;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    private final NamespacedKey ACTION_KEY;

    public WarpCommand(SchulCorePlugin plugin) {
        this.plugin = plugin;
        this.ACTION_KEY = new NamespacedKey(plugin, "rtp_action");
        // Register as listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private String toSmallCaps(String text) {
        return text.replace("A", "ᴀ").replace("B", "ʙ").replace("C", "ᴄ")
                .replace("D", "ᴅ").replace("E", "ᴇ").replace("F", "ғ")
                .replace("G", "ɢ").replace("H", "ʜ").replace("I", "ɪ")
                .replace("J", "ᴊ").replace("K", "ᴋ").replace("L", "ʟ")
                .replace("M", "ᴍ").replace("N", "ɴ").replace("O", "ᴏ")
                .replace("P", "ᴘ").replace("Q", "ǫ").replace("R", "ʀ")
                .replace("S", "s").replace("T", "ᴛ").replace("U", "ᴜ")
                .replace("V", "ᴠ").replace("W", "ᴡ").replace("X", "x")
                .replace("Y", "ʏ").replace("Z", "ᴢ");
    }

    // ==================== COMMAND ====================

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }
        Player p = (Player) sender;

        // Combat check
        CombatManager combatManager = plugin.getCombatManager();
        if (combatManager != null && combatManager.isInCombat(p)) {
            p.sendMessage("§8┃ §a§lRTP §8┃ §cDu bist im Kampf!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }

        // Cooldown check
        if (cooldowns.containsKey(p.getUniqueId())) {
            long remaining = (cooldowns.get(p.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (remaining > 0) {
                p.sendMessage("§8┃ §a§lRTP §8┃ §cBitte warte noch §e" + remaining + "s§c!");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            cooldowns.remove(p.getUniqueId());
        }

        p.openInventory(createRtpGUI());
        return true;
    }

    // ==================== GUI ====================

    private Inventory createRtpGUI() {
        Inventory inv = Bukkit.createInventory(null, 27, "§a§l" + toSmallCaps("RANDOM TELEPORT"));

        // Fill borders
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("§8⬛");
        border.setItemMeta(borderMeta);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        // Slot 11: Nether
        ItemStack nether = mark(new ItemStack(Material.NETHERRACK), "rtp_nether");
        ItemMeta netherMeta = nether.getItemMeta();
        netherMeta.setDisplayName("§c§l🔥 " + toSmallCaps("NETHER"));
        List<String> netherLore = new ArrayList<>();
        netherLore.add("§8");
        netherLore.add("§7Teleportiere dich zu einer");
        netherLore.add("§7zufälligen Position im §cNether§7.");
        netherLore.add("§8");
        netherLore.add("§7Radius: §f" + RTP_RADIUS + " Blöcke");
        netherLore.add("§8");
        netherLore.add("§e▸ Klicken zum Teleportieren");
        netherMeta.setLore(netherLore);
        nether.setItemMeta(netherMeta);
        inv.setItem(11, nether);

        // Slot 13: Farm Welt
        ItemStack farm = mark(new ItemStack(Material.HAY_BLOCK), "rtp_farm");
        ItemMeta farmMeta = farm.getItemMeta();
        farmMeta.setDisplayName("§a§l🌾 " + toSmallCaps("FARM WELT"));
        List<String> farmLore = new ArrayList<>();
        farmLore.add("§8");
        farmLore.add("§7Teleportiere dich zu einer");
        farmLore.add("§7zufälligen Position in der §aFarm Welt§7.");
        farmLore.add("§8");
        farmLore.add("§7Radius: §f" + RTP_RADIUS + " Blöcke");
        farmLore.add("§8");
        farmLore.add("§e▸ Klicken zum Teleportieren");
        farmMeta.setLore(farmLore);
        farm.setItemMeta(farmMeta);
        inv.setItem(13, farm);

        // Slot 15: End (Coming Soon)
        ItemStack end = mark(new ItemStack(Material.END_STONE), "rtp_end");
        ItemMeta endMeta = end.getItemMeta();
        endMeta.setDisplayName("§5§l⭐ " + toSmallCaps("THE END"));
        List<String> endLore = new ArrayList<>();
        endLore.add("§8");
        endLore.add("§7Das End wird bald verfügbar sein!");
        endLore.add("§8");
        endLore.add("§c§l✖ " + toSmallCaps("COMING SOON"));
        endMeta.setLore(endLore);
        end.setItemMeta(endMeta);
        inv.setItem(15, end);

        return inv;
    }

    // ==================== LISTENER ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();

        String title = e.getView().getTitle();
        if (!title.contains("ʀᴀɴᴅᴏᴍ ᴛᴇʟᴇᴘᴏʀᴛ")) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String action = getAction(clicked);
        if (action == null) return;

        // Combat check in click handler
        CombatManager cm = plugin.getCombatManager();
        if (cm != null && cm.isInCombat(player)) {
            player.sendMessage("§8┃ §a§lRTP §8┃ §cDu bist im Kampf!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.closeInventory();
            return;
        }

        // Cooldown check in click handler
        if (cooldowns.containsKey(player.getUniqueId())) {
            long rem = (cooldowns.get(player.getUniqueId()) - System.currentTimeMillis()) / 1000;
            if (rem > 0) {
                player.sendMessage("§8┃ §a§lRTP §8┃ §cBitte warte noch §e" + rem + "s§c!");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                player.closeInventory();
                return;
            }
            cooldowns.remove(player.getUniqueId());
        }

        // Cooldown sofort setzen um Spam zu verhindern
        if (action.equals("rtp_nether") || action.equals("rtp_farm")) {
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (COOLDOWN_SECONDS * 1000L));
        }

        switch (action) {
            case "rtp_nether":
                player.closeInventory();
                performRtp(player, World.Environment.NETHER);
                break;

            case "rtp_farm":
                player.closeInventory();
                performRtpFarm(player);
                break;

            case "rtp_end":
                player.sendMessage("§8┃ §5§lEND §8┃ §7Das End ist noch nicht verfügbar. §c§lCOMING SOON!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                break;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        String title = e.getView().getTitle();
        if (title.contains("ʀᴀɴᴅᴏᴍ ᴛᴇʟᴇᴘᴏʀᴛ")) {
            e.setCancelled(true);
        }
    }

    // ==================== RTP LOGIC ====================

    private void performRtp(Player player, World.Environment environment) {
        // Combat check before teleport
        CombatManager combatManager = plugin.getCombatManager();
        if (combatManager != null && combatManager.isInCombat(player)) {
            player.sendMessage("§8┃ §a§lRTP §8┃ §cDu bist im Kampf!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        World world = null;
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == environment) {
                world = w;
                break;
            }
        }

        if (world == null) {
            player.sendMessage("§8┃ §a§lRTP §8┃ §cWelt nicht gefunden!");
            return;
        }

        doRtp(player, world);
    }

    private void performRtpFarm(Player player) {
        // Combat check before teleport
        CombatManager combatManager = plugin.getCombatManager();
        if (combatManager != null && combatManager.isInCombat(player)) {
            player.sendMessage("§8┃ §a§lRTP §8┃ §cDu bist im Kampf!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Try common farm world names
        World farmWorld = Bukkit.getWorld("farm");
        if (farmWorld == null) farmWorld = Bukkit.getWorld("farmwelt");
        if (farmWorld == null) farmWorld = Bukkit.getWorld("FarmWelt");
        if (farmWorld == null) farmWorld = Bukkit.getWorld("Farm");
        if (farmWorld == null) farmWorld = Bukkit.getWorld("farmworld");

        // Fallback: search for a world with "farm" in name
        if (farmWorld == null) {
            for (World w : Bukkit.getWorlds()) {
                if (w.getName().toLowerCase().contains("farm")) {
                    farmWorld = w;
                    break;
                }
            }
        }

        if (farmWorld == null) {
            player.sendMessage("§8┃ §a§lRTP §8┃ §cFarm-Welt nicht gefunden!");
            player.sendMessage("§8  ▸ §7Verfügbare Welten:");
            for (World w : Bukkit.getWorlds()) {
                player.sendMessage("§8    ▸ §f" + w.getName());
            }
            return;
        }

        doRtp(player, farmWorld);
    }

    private void doRtp(Player player, World world) {
        player.sendMessage("§8┃ §a§lRTP §8┃ §7Suche sichere Position in §e" + world.getName() + "§7...");

        // Async: Sichere Position finden
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location safeLoc = findSafeLocation(world);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (safeLoc == null) {
                    player.sendMessage("§8┃ §a§lRTP §8┃ §cKeine sichere Position gefunden. Versuche es erneut!");
                    cooldowns.remove(player.getUniqueId());
                    return;
                }

                // Countdown starten
                player.sendMessage("§8┃ §a§lRTP §8┃ §7Teleport in §e" + COUNTDOWN_SECONDS + "s§7... Nicht bewegen!");
                final Location startLoc = player.getLocation().clone();
                final UUID uid = player.getUniqueId();

                BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                    int remaining = COUNTDOWN_SECONDS;
                    @Override
                    public void run() {
                        // Spieler offline?
                        if (!player.isOnline()) {
                            cancelPending(uid);
                            return;
                        }
                        // Combat check
                        CombatManager cm = plugin.getCombatManager();
                        if (cm != null && cm.isInCombat(player)) {
                            player.sendMessage("§8┃ §a§lRTP §8┃ §cTeleport abgebrochen! Du bist im Kampf.");
                            player.sendTitle("§c✖ Abgebrochen", "§7Kampf!", 0, 30, 10);
                            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                            cancelPending(uid);
                            cooldowns.remove(uid);
                            return;
                        }

                        if (remaining <= 0) {
                            // Teleport!
                            player.teleport(safeLoc);
                            player.sendMessage("§8┃ §a§lRTP §8┃ §aTeleportiert nach §e" + world.getName() + "§a!");
                            player.sendMessage("§8  ▸ §7Position: §f" + safeLoc.getBlockX() + "§8, §f" + safeLoc.getBlockY() + "§8, §f" + safeLoc.getBlockZ());
                            player.sendTitle("§a✔ Teleportiert", "§7" + world.getName(), 0, 40, 10);
                            try {
                                player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, player.getLocation().add(0, 1, 0), 50);
                                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                            } catch (Throwable ignored) {}
                            cancelPending(uid);
                            return;
                        }

                        // Countdown Title anzeigen
                        String color = remaining <= 2 ? "§c" : remaining <= 3 ? "§e" : "§a";
                        player.sendTitle(color + "§l" + remaining, "§7Nicht bewegen!", 0, 25, 0);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, remaining <= 2 ? 1.5f : 1f);
                        remaining--;
                    }
                }, 0L, 20L); // Jede Sekunde

                pendingTeleports.put(uid, task);
            });
        });
    }

    /**
     * Bricht einen laufenden RTP-Countdown ab.
     */
    private void cancelPending(UUID uid) {
        BukkitTask task = pendingTeleports.remove(uid);
        if (task != null) task.cancel();
    }

    /**
     * Bewegung während Countdown = Abbruch
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent e) {
        UUID uid = e.getPlayer().getUniqueId();
        if (!pendingTeleports.containsKey(uid)) return;
        // Nur echte Bewegung (nicht Kopfdrehen)
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            cancelPending(uid);
            cooldowns.remove(uid);
            Player p = e.getPlayer();
            p.sendMessage("§8┃ §a§lRTP §8┃ §cTeleport abgebrochen! Du hast dich bewegt.");
            p.sendTitle("§c✖ Abgebrochen", "§7Bewegt!", 0, 30, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    // ==================== SAFE LOCATION ====================

    private Location findSafeLocation(World world) {
        int maxAttempts = 30;

        for (int i = 0; i < maxAttempts; i++) {
            int x = random.nextInt(RTP_RADIUS * 2) - RTP_RADIUS;
            int z = random.nextInt(RTP_RADIUS * 2) - RTP_RADIUS;

            // Mindestdistanz vom Spawn
            if (Math.abs(x) < RTP_MIN && Math.abs(z) < RTP_MIN) continue;

            int y;
            if (world.getEnvironment() == World.Environment.NETHER) {
                y = findNetherSafeY(world, x, z);
            } else {
                y = world.getHighestBlockYAt(x, z) + 1;
            }

            if (y < 1 || y > 300) continue;

            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (isSafeLocation(loc)) {
                return loc;
            }
        }

        return null;
    }

    private int findNetherSafeY(World world, int x, int z) {
        for (int y = 100; y > 30; y--) {
            Location check = new Location(world, x, y, z);
            if (check.getBlock().getType().isAir()
                && check.clone().add(0, 1, 0).getBlock().getType().isAir()
                && check.clone().add(0, -1, 0).getBlock().getType().isSolid()) {
                return y;
            }
        }
        return -1;
    }

    private boolean isSafeLocation(Location loc) {
        org.bukkit.block.Block feet = loc.getBlock();
        org.bukkit.block.Block head = loc.clone().add(0, 1, 0).getBlock();
        org.bukkit.block.Block ground = loc.clone().add(0, -1, 0).getBlock();

        if (!feet.getType().isAir() || !head.getType().isAir()) return false;
        if (!ground.getType().isSolid()) return false;

        String groundName = ground.getType().name();
        if (groundName.contains("LAVA") || groundName.contains("WATER") || groundName.contains("MAGMA")) return false;

        return true;
    }

    // ==================== PDC HELPERS ====================

    private ItemStack mark(ItemStack item, String action) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
    }
}
