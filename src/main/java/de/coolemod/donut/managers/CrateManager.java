package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Neuer CrateManager:
 * - Lädt Kisten aus config (display, key, guaranteed, pool mit Gewichtungen)
 * - Erzeugt Schlüssel als Items mit PDC (crate_id)
 * - Öffnet Kisten mit GUI-Animation (scrolling durch mögliche Items)
 */
public class CrateManager {
    private final DonutPlugin plugin;
    private final Random random = new Random();

    private final Map<String, Crate> crates = new HashMap<>();

    public CrateManager(DonutPlugin plugin) {
        this.plugin = plugin;
        loadCrates();
    }

    public void reload() {
        crates.clear();
        loadCrates();
    }

    private void loadCrates() {
        if (!plugin.getConfig().isConfigurationSection("crates")) return;
        for (String id : plugin.getConfig().getConfigurationSection("crates").getKeys(false)) {
            try {
                String base = "crates." + id;
                String display = plugin.getConfig().getString(base + ".display", id);
                String keyName = plugin.getConfig().getString(base + ".key", "Key");
                String tier = plugin.getConfig().getString(base + ".tier", null);
                int ticks = plugin.getConfig().getInt(base + ".animation.ticks", 2);
                int cycles = plugin.getConfig().getInt(base + ".animation.cycles", 30);
                List<String> raw = plugin.getConfig().getStringList(base + ".pool");
                List<String> guaranteedList = plugin.getConfig().getStringList(base + ".guaranteed");
                List<String> bundlesRaw = plugin.getConfig().getStringList(base + ".bundles");
                Crate c = new Crate(id, display, keyName, tier, ticks, cycles);
                // guaranteed entries (can be empty)
                for (String g : guaranteedList) {
                    PoolEntry ge = parsePoolEntry(g);
                    if (ge != null) c.guaranteed.add(ge);
                }
                // pool
                for (String s : raw) {
                    PoolEntry e = parsePoolEntry(s);
                    if (e != null) c.pool.add(e);
                }
                // bundles (semicolon separated list of ITEM:AMOUNT entries)
                for (String b : bundlesRaw) {
                    List<ItemStack> bundleItems = parseBundleString(b);
                    if (!bundleItems.isEmpty()) c.bundles.add(bundleItems);
                }
                if (!c.pool.isEmpty() || !c.guaranteed.isEmpty() || !c.bundles.isEmpty()) crates.put(id, c);
            } catch (Exception ex) {
                plugin.getLogger().warning("Fehler beim Laden der Crate: " + id + " - " + ex.getMessage());
            }
        }
    }

    private PoolEntry parsePoolEntry(String s) {
        // Supported formats:
        // MATERIAL:amount
        // MATERIAL:amount:weight
        // MATERIAL:amount:namedWorthKey
        // MATERIAL:amount:weight:namedWorthKey
        try {
            String[] parts = s.split(":" );
            if (parts.length < 2) return null;
            Material m = Material.valueOf(parts[0]);
            int amount = Integer.parseInt(parts[1]);
            int weight = 1;
            String namedKey = null;
            if (parts.length >= 3) {
                try {
                    weight = Integer.parseInt(parts[2]);
                } catch (NumberFormatException nfe) {
                    namedKey = parts[2];
                }
            }
            if (parts.length >= 4) {
                // parts[2] is weight, parts[3] is namedKey
                try { weight = Integer.parseInt(parts[2]); } catch (NumberFormatException ignored) {}
                namedKey = parts[3];
            }
            ItemStack it = new ItemStack(m, amount);
            if (namedKey != null) {
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "donut_named_worth"), PersistentDataType.STRING, namedKey);
                    it.setItemMeta(meta);
                }
            }
            return new PoolEntry(it, weight);
        } catch (Exception ex) {
            plugin.getLogger().warning("Ungültige Crate-Pool-Entry: " + s + " - " + ex.getMessage());
            return null;
        }
    }

    private List<ItemStack> parseBundleString(String s) {
        List<ItemStack> list = new ArrayList<>();
        if (s == null || s.trim().isEmpty()) return list;
        String[] parts = s.split(";");
        for (String p : parts) {
            String[] itm = p.split(":" );
            try {
                if (itm.length < 2) continue;
                Material m = Material.valueOf(itm[0]);
                int amount = Integer.parseInt(itm[1]);
                ItemStack is = new ItemStack(m, amount);
                list.add(is);
            } catch (Exception ex) {
                plugin.getLogger().warning("Ungültige Bundle-Entry in Crate: " + p);
            }
        }
        return list;
    }

    public Set<String> getCrateIds() { return Collections.unmodifiableSet(crates.keySet()); }

    public boolean hasCrate(String id) { return crates.containsKey(id); }

    public Crate getCrate(String id) { return crates.get(id); }

    /**
     * Zählt wie viele Keys ein Spieler für eine bestimmte Kiste hat.
     */
    public int getKeyCount(UUID playerId, String crateId) {
        Player p = Bukkit.getPlayer(playerId);
        if (p == null) return 0;
        int count = 0;
        for (ItemStack is : p.getInventory().getContents()) {
            if (is == null) continue;
            if (is.getType() == Material.TRIPWIRE_HOOK && is.hasItemMeta()) {
                ItemMeta meta = is.getItemMeta();
                if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_crate_id"), PersistentDataType.STRING)) {
                    String id = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "donut_crate_id"), PersistentDataType.STRING);
                    if (crateId.equals(id)) count += is.getAmount();
                }
            }
        }
        return count;
    }

    /**
     * Gibt einem Spieler Keys für eine Kiste.
     */
    public void giveKeys(UUID playerId, String crateId, int amount) {
        Player p = Bukkit.getPlayer(playerId);
        if (p == null) return;
        ItemStack key = createKey(crateId, amount);
        if (p.getInventory().firstEmpty() == -1) {
            p.getWorld().dropItemNaturally(p.getLocation(), key);
        } else {
            p.getInventory().addItem(key);
        }
    }

    public ItemStack createKey(String crateId, int amount) {
        String keyName = crateId;
        Crate c = crates.get(crateId);
        if (c != null) keyName = c.keyName;
        ItemStack key = new ItemStack(Material.TRIPWIRE_HOOK, amount);
        ItemMeta m = key.getItemMeta();
        m.setDisplayName("§6" + keyName);
        m.setLore(Arrays.asList("§7Rechtsklick zum Öffnen: " + (c != null ? c.display : crateId)));
        m.getPersistentDataContainer().set(new NamespacedKey(plugin, "donut_crate_id"), PersistentDataType.STRING, crateId);
        key.setItemMeta(m);
        return key;
    }

    public boolean consumeKey(Player p, String crateId) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack is = p.getInventory().getItem(i);
            if (is == null) continue;
            if (is.getType() == Material.TRIPWIRE_HOOK && is.hasItemMeta() && is.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_crate_id"), PersistentDataType.STRING)) {
                String id = is.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "donut_crate_id"), PersistentDataType.STRING);
                if (crateId.equals(id)) {
                    is.setAmount(is.getAmount() - 1);
                    if (is.getAmount() <= 0) p.getInventory().setItem(i, null);
                    else p.getInventory().setItem(i, is);
                    return true;
                }
            }
        }
        return false;
    }

    private ItemStack pickWeighted(Crate c) {
        if (c.pool.isEmpty()) return null;
        int total = c.pool.stream().mapToInt(pe -> pe.weight).sum();
        int r = random.nextInt(total) + 1;
        int acc = 0;
        for (PoolEntry pe : c.pool) {
            acc += pe.weight;
            if (r <= acc) return pe.item.clone();
        }
        return c.pool.getFirst().item.clone();
    }

    public void openCrateAnimated(Player p, String crateId) {
        openCrateAnimated(p, crateId, false);
    }

    public void openCrateAnimated(Player p, String crateId, boolean skipKeyCheck) {
        if (!crates.containsKey(crateId)) { p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cKiste nicht gefunden."); return; }
        Crate c = crates.get(crateId);
        // Prüfe Schlüssel (außer bei Admin-Test)
        if (!skipKeyCheck && !consumeKey(p, crateId)) { 
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cDu hast keinen passenden Schlüssel."); 
            return; 
        }

        Inventory inv = Bukkit.createInventory(null, 27, "§6Öffne: " + c.display);
        GUI_FILL: {
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta gm = glass.getItemMeta(); gm.setDisplayName("§7"); glass.setItemMeta(gm);
            for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, glass);
            // Mitte zeigt Animation
        }

        p.openInventory(inv);

        // Animation: cycles steps, each tick replace the center item with random pool item; after cycles pick reward
        AtomicInteger counter = new AtomicInteger(0);
        List<ItemStack> displayPool = c.pool.stream().map(pe -> pe.item.clone()).collect(Collectors.toList());
        final java.util.concurrent.atomic.AtomicReference<BukkitTask> taskRef = new java.util.concurrent.atomic.AtomicReference<>();
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int count = counter.incrementAndGet();
            int idx = (displayPool.isEmpty() ? 0 : (count % displayPool.size()));
            if (!displayPool.isEmpty()) inv.setItem(13, displayPool.get(idx));
            if (count >= c.cycles) {
                if (taskRef.get() != null) taskRef.get().cancel();
                List<ItemStack> rewards = new ArrayList<>();
                for (PoolEntry ge : c.guaranteed) rewards.add(ge.item.clone());
                if (!c.bundles.isEmpty()) {
                    List<ItemStack> chosenBundle = c.bundles.get(random.nextInt(c.bundles.size()));
                    for (ItemStack bi : chosenBundle) rewards.add(bi.clone());
                }
                ItemStack main = pickWeighted(c);
                if (main != null) rewards.add(main.clone());
                for (int i = 0; i < rewards.size(); i++) {
                    rewards.set(i, applyTierEnchantIfNeeded(rewards.get(i), c.tier));
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    p.closeInventory();
                    StringBuilder names = new StringBuilder();
                    for (ItemStack reward : rewards) {
                        if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), reward);
                        else p.getInventory().addItem(reward);
                        if (names.length() > 0) names.append(", ");
                        names.append(reward.getAmount()).append("x ").append(reward.getType().name());
                    }
                    p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§aDu hast erhalten: §e" + names.toString());
                    p.getWorld().playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            org.bukkit.Particle part;
                            try { part = org.bukkit.Particle.valueOf("FIREWORKS_SPARK"); } catch (IllegalArgumentException ex) { part = org.bukkit.Particle.CLOUD; }
                            p.getWorld().spawnParticle(part, p.getLocation().add(0,1,0), 40);
                        } catch (Throwable t) {
                            p.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, p.getLocation().add(0,1,0), 40);
                        }
                    }, 2L);
                });
            }
        }, 0L, c.ticks);
        taskRef.set(task);
    }

    private ItemStack applyTierEnchantIfNeeded(ItemStack item, String tier) {
        if (item == null || tier == null) return item;
        try {
            String base = "settings.crate-tiers." + tier;
            if (!plugin.getConfig().isConfigurationSection(base)) return item;
            int maxLevel = plugin.getConfig().getInt(base + ".enchant-level", 0);
            double chance = plugin.getConfig().getDouble(base + ".enchant-chance", 0.0);
            int minEnchants = plugin.getConfig().getInt(base + ".min-enchants", 1);
            int maxEnchants = plugin.getConfig().getInt(base + ".max-enchants", 3);
            if (maxLevel <= 0 || chance <= 0.0) return item;

            // Collect all possible enchantments for this item
            List<org.bukkit.enchantments.Enchantment> possible = new ArrayList<>();
            for (org.bukkit.enchantments.Enchantment e : org.bukkit.enchantments.Enchantment.values()) {
                try {
                    if (e.canEnchantItem(item)) possible.add(e);
                } catch (Throwable ignored) {}
            }
            if (possible.isEmpty()) return item;
            
            ItemStack clone = item.clone();
            java.util.Collections.shuffle(possible);
            
            // Determine how many enchantments to add
            int numEnchants = minEnchants + random.nextInt(Math.max(1, maxEnchants - minEnchants + 1));
            int added = 0;
            
            for (org.bukkit.enchantments.Enchantment ench : possible) {
                if (added >= numEnchants) break;
                if (random.nextDouble() > chance) continue;
                
                // Calculate level based on tier
                int enchMaxLevel = Math.min(maxLevel, ench.getMaxLevel());
                int lvl = Math.max(1, random.nextInt(enchMaxLevel) + 1);
                
                // Check for conflicts with existing enchantments
                boolean conflicts = false;
                for (org.bukkit.enchantments.Enchantment existing : clone.getEnchantments().keySet()) {
                    if (ench.conflictsWith(existing)) {
                        conflicts = true;
                        break;
                    }
                }
                if (conflicts) continue;
                
                clone.addUnsafeEnchantment(ench, lvl);
                added++;
            }
            
            return clone;
        } catch (Exception ex) {
            plugin.getLogger().warning("Fehler beim Anwenden von Tier-Enchant auf Item: " + ex.getMessage());
            return item;
        }
    }

    // helper classes
    public static class Crate {
        public final String id;
        public final String display;
        public final String keyName;
        public final int ticks;
        public final int cycles;
        public final List<PoolEntry> pool = new ArrayList<>();
        public final List<PoolEntry> guaranteed = new ArrayList<>();
        public final List<List<ItemStack>> bundles = new ArrayList<>();

        public final String tier;

        public Crate(String id, String display, String keyName, String tier, int ticks, int cycles) {
            this.id = id; this.display = display; this.keyName = keyName; this.tier = tier; this.ticks = ticks; this.cycles = cycles;
        }
    }

    public static class PoolEntry {
        public final ItemStack item;
        public final int weight;
        public PoolEntry(ItemStack item, int weight) { this.item = item; this.weight = weight; }
    }
}
