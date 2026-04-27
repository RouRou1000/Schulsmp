package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
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

    // Virtual key storage: playerUUID -> crateId -> amount
    private final Map<UUID, Map<String, Integer>> virtualKeys = new HashMap<>();
    private File keyFile;
    private FileConfiguration keyConfig;

    public CrateManager(DonutPlugin plugin) {
        this.plugin = plugin;
        loadCrates();
        loadVirtualKeys();
    }

    public void reload() {
        crates.clear();
        loadCrates();
    }

    // ==================== VIRTUAL KEY STORAGE ====================

    private void loadVirtualKeys() {
        keyFile = new File(plugin.getDataFolder(), "crate_keys.yml");
        if (!keyFile.exists()) {
            try { keyFile.createNewFile(); } catch (IOException ignored) {}
        }
        keyConfig = YamlConfiguration.loadConfiguration(keyFile);
        if (!keyConfig.isConfigurationSection("keys")) return;
        for (String uuidStr : keyConfig.getConfigurationSection("keys").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                var section = keyConfig.getConfigurationSection("keys." + uuidStr);
                if (section == null) continue;
                Map<String, Integer> playerKeys = new HashMap<>();
                for (String crateId : section.getKeys(false)) {
                    int amount = keyConfig.getInt("keys." + uuidStr + "." + crateId);
                    if (amount > 0) playerKeys.put(crateId, amount);
                }
                if (!playerKeys.isEmpty()) virtualKeys.put(uuid, playerKeys);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveVirtualKeys() {
        if (keyConfig == null) return;
        for (Map.Entry<UUID, Map<String, Integer>> e : virtualKeys.entrySet()) {
            String uuidStr = e.getKey().toString();
            for (Map.Entry<String, Integer> ce : e.getValue().entrySet()) {
                keyConfig.set("keys." + uuidStr + "." + ce.getKey(), ce.getValue());
            }
        }
        try { keyConfig.save(keyFile); } catch (IOException ex) {
            plugin.getLogger().warning("Konnte crate_keys.yml nicht speichern: " + ex.getMessage());
        }
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
     * Gibt die Anzahl virtueller Keys eines Spielers für eine Kiste zurück.
     */
    public int getKeyCount(UUID playerId, String crateId) {
        Map<String, Integer> playerKeys = virtualKeys.get(playerId);
        if (playerKeys == null) return 0;
        return playerKeys.getOrDefault(crateId, 0);
    }

    /**
     * Gibt einem Spieler Keys für eine Kiste (virtuell gespeichert, kein Item).
     */
    public void giveKeys(UUID playerId, String crateId, int amount) {
        virtualKeys.computeIfAbsent(playerId, k -> new HashMap<>())
                   .merge(crateId, amount, Integer::sum);
        saveVirtualKeys();
        Player p = Bukkit.getPlayer(playerId);
        if (p != null) {
            String displayName = crates.containsKey(crateId) ? crates.get(crateId).display : crateId;
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") +
                "§a+" + amount + " §6Schlüssel §afür §e" + displayName + " §aerhalten!");
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
        Map<String, Integer> playerKeys = virtualKeys.get(p.getUniqueId());
        if (playerKeys == null) return false;
        int count = playerKeys.getOrDefault(crateId, 0);
        if (count <= 0) return false;
        if (count == 1) playerKeys.remove(crateId);
        else playerKeys.put(crateId, count - 1);
        saveVirtualKeys();
        return true;
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
        if (!crates.containsKey(crateId)) {
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cKiste nicht gefunden.");
            return;
        }

        Crate c = crates.get(crateId);
        if (!skipKeyCheck && !consumeKey(p, crateId)) {
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cDu hast keinen passenden Schlüssel.");
            return;
        }

        ItemStack reward = createFixedReward(c);
        if (reward == null) {
            reward = pickWeighted(c);
            if (reward != null) {
                reward = applyTierEnchantIfNeeded(reward, c.tier);
            }
        }
        if (reward == null) {
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cFür diese Kiste ist keine Belohnung konfiguriert.");
            return;
        }

        if (p.getInventory().firstEmpty() == -1) {
            p.getWorld().dropItemNaturally(p.getLocation(), reward);
        } else {
            p.getInventory().addItem(reward);
        }

        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§aDu hast erhalten: §e" + reward.getAmount() + "x " + formatMaterialName(reward.getType()));
        p.getWorld().playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.15f);
        try {
            org.bukkit.Particle part;
            try {
                part = org.bukkit.Particle.valueOf("FIREWORKS_SPARK");
            } catch (IllegalArgumentException ex) {
                part = org.bukkit.Particle.CLOUD;
            }
            p.getWorld().spawnParticle(part, p.getLocation().add(0, 1, 0), 40);
        } catch (Throwable ignored) {
            p.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, p.getLocation().add(0, 1, 0), 40);
        }
    }

    public List<ItemStack> getPreviewItems(String crateId) {
        Crate crate = crates.get(crateId);
        if (crate == null) {
            return List.of();
        }

        String tier = normalizeTier(crate.tier, crate.id);
        if (tier.equals("basic")) {
            return buildGearPool(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, tier);
        }
        if (tier.equals("rare")) {
            return buildGearPool(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, tier);
        }
        if (tier.equals("legendary")) {
            return buildGearPool(Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS, Material.NETHERITE_SWORD, Material.NETHERITE_PICKAXE, tier);
        }

        return crate.pool.stream().map(entry -> entry.item.clone()).toList();
    }

    public List<String> getTierDescription(String crateId) {
        Crate crate = crates.get(crateId);
        if (crate == null) {
            return List.of();
        }

        String tier = normalizeTier(crate.tier, crate.id);
        List<String> lines = new ArrayList<>();
        switch (tier) {
            case "basic" -> {
                lines.add("§7Belohnung: §fWähle 1 Teil");
                lines.add("§7Rüstung: §bDiamond + Protection II");
                lines.add("§7Schwert: §bDiamond + Sharpness II");
                lines.add("§7Pickaxe: §bDiamond + Effi II (Fortune/Silk Touch)");
            }
            case "rare" -> {
                lines.add("§7Belohnung: §fWähle 1 Teil");
                lines.add("§7Rüstung: §dDiamond + volle Enchants");
                lines.add("§7Schwert: §dDiamond + volle Enchants");
                lines.add("§7Pickaxe: §dDiamond + Effi V (Fortune/Silk Touch)");
            }
            case "legendary" -> {
                lines.add("§7Belohnung: §fWähle 1 Teil");
                lines.add("§7Rüstung: §6Netherite + volle Enchants");
                lines.add("§7Schwert: §6Netherite + volle Enchants");
                lines.add("§7Pickaxe: §6Netherite + Effi V (Fortune/Silk Touch)");
            }
            default -> lines.add("§7Belohnung basiert auf dem konfigurierten Pool.");
        }
        return lines;
    }

    private ItemStack createFixedReward(Crate crate) {
        List<ItemStack> pool = getPreviewItems(crate.id);
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(random.nextInt(pool.size())).clone();
    }

    private List<ItemStack> buildGearPool(Material helmet, Material chestplate, Material leggings, Material boots, Material sword, Material pickaxe, String tier) {
        List<ItemStack> pool = new ArrayList<>();
        pool.add(createTierPiece(helmet, tier, null));
        pool.add(createTierPiece(chestplate, tier, null));
        pool.add(createTierPiece(leggings, tier, null));
        pool.add(createTierPiece(boots, tier, null));
        pool.add(createTierPiece(sword, tier, null));
        pool.add(createTierPiece(pickaxe, tier, "fortune"));
        pool.add(createTierPiece(pickaxe, tier, "silktouch"));
        return pool;
    }

    private ItemStack createTierPiece(Material material, String tier, String variant) {
        ItemStack item = new ItemStack(material);
        String name = material.name();
        if (name.endsWith("SWORD")) {
            switch (tier) {
                case "basic" -> applyBasicSwordEnchants(item);
                case "rare", "legendary" -> applyAdvancedSwordEnchants(item);
            }
        } else if (name.endsWith("PICKAXE") && "silktouch".equals(variant)) {
            switch (tier) {
                case "basic" -> applyBasicSilkTouchPickaxeEnchants(item);
                case "rare", "legendary" -> applyAdvancedSilkTouchPickaxeEnchants(item);
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + formatMaterialName(material) + " §7(Silk Touch)");
                item.setItemMeta(meta);
            }
        } else if (name.endsWith("PICKAXE")) {
            switch (tier) {
                case "basic" -> applyBasicPickaxeEnchants(item);
                case "rare", "legendary" -> applyAdvancedPickaxeEnchants(item);
            }
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§b" + formatMaterialName(material) + " §7(Fortune)");
                item.setItemMeta(meta);
            }
        } else {
            switch (tier) {
                case "basic" -> applyBasicArmorEnchants(item);
                case "rare", "legendary" -> applyAdvancedArmorEnchants(item);
            }
        }
        return item;
    }

    private void applyBasicArmorEnchants(ItemStack item) {
        addEnchant(item, org.bukkit.enchantments.Enchantment.PROTECTION, 2);
        if (item.getType() == Material.DIAMOND_LEGGINGS) {
            addEnchant(item, org.bukkit.enchantments.Enchantment.SWIFT_SNEAK, 2);
        }
        if (item.getType() == Material.DIAMOND_BOOTS) {
            addEnchant(item, org.bukkit.enchantments.Enchantment.DEPTH_STRIDER, 2);
        }
    }

    private void applyAdvancedArmorEnchants(ItemStack item) {
        addEnchant(item, org.bukkit.enchantments.Enchantment.PROTECTION, 4);
        addEnchant(item, org.bukkit.enchantments.Enchantment.UNBREAKING, 3);
        addEnchant(item, org.bukkit.enchantments.Enchantment.MENDING, 1);

        if (item.getType().name().endsWith("HELMET")) {
            addEnchant(item, org.bukkit.enchantments.Enchantment.RESPIRATION, 3);
            addEnchant(item, org.bukkit.enchantments.Enchantment.AQUA_AFFINITY, 1);
        }
        if (item.getType().name().endsWith("CHESTPLATE")) {
            addEnchant(item, org.bukkit.enchantments.Enchantment.THORNS, 2);
        }
        if (item.getType().name().endsWith("LEGGINGS")) {
            addEnchant(item, org.bukkit.enchantments.Enchantment.SWIFT_SNEAK, 3);
        }
        if (item.getType().name().endsWith("BOOTS")) {
            addEnchant(item, org.bukkit.enchantments.Enchantment.FEATHER_FALLING, 4);
            addEnchant(item, org.bukkit.enchantments.Enchantment.DEPTH_STRIDER, 3);
        }
    }

    private void applyBasicSwordEnchants(ItemStack item) {
        addEnchant(item, org.bukkit.enchantments.Enchantment.SHARPNESS, 2);
        addEnchant(item, org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
    }

    private void applyAdvancedSwordEnchants(ItemStack item) {
        addEnchant(item, org.bukkit.enchantments.Enchantment.SHARPNESS, 5);
        addEnchant(item, org.bukkit.enchantments.Enchantment.UNBREAKING, 3);
        addEnchant(item, org.bukkit.enchantments.Enchantment.MENDING, 1);
        addEnchant(item, org.bukkit.enchantments.Enchantment.LOOTING, 3);
        addEnchant(item, org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 2);
        addEnchant(item, org.bukkit.enchantments.Enchantment.SWEEPING_EDGE, 3);
    }

    private void applyBasicPickaxeEnchants(ItemStack item) {
        addEnchant(item, org.bukkit.enchantments.Enchantment.EFFICIENCY, 2);
        addEnchant(item, org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
    }

    private void applyAdvancedPickaxeEnchants(ItemStack item) {
        addEnchant(item, org.bukkit.enchantments.Enchantment.EFFICIENCY, 5);
        addEnchant(item, org.bukkit.enchantments.Enchantment.UNBREAKING, 3);
        addEnchant(item, org.bukkit.enchantments.Enchantment.MENDING, 1);
        addEnchant(item, org.bukkit.enchantments.Enchantment.FORTUNE, 3);
    }

    private void applyBasicSilkTouchPickaxeEnchants(ItemStack item) {
        addEnchant(item, org.bukkit.enchantments.Enchantment.EFFICIENCY, 2);
        addEnchant(item, org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
        addEnchant(item, org.bukkit.enchantments.Enchantment.SILK_TOUCH, 1);
    }

    private void applyAdvancedSilkTouchPickaxeEnchants(ItemStack item) {
        addEnchant(item, org.bukkit.enchantments.Enchantment.EFFICIENCY, 5);
        addEnchant(item, org.bukkit.enchantments.Enchantment.UNBREAKING, 3);
        addEnchant(item, org.bukkit.enchantments.Enchantment.MENDING, 1);
        addEnchant(item, org.bukkit.enchantments.Enchantment.SILK_TOUCH, 1);
    }

    private void addEnchant(ItemStack item, org.bukkit.enchantments.Enchantment enchantment, int level) {
        if (item == null || enchantment == null) {
            return;
        }
        try {
            item.addUnsafeEnchantment(enchantment, level);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private String normalizeTier(String tier, String fallback) {
        String value = tier == null || tier.isBlank() ? fallback : tier;
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String formatMaterialName(Material material) {
        String[] parts = material.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(part.charAt(0)).append(part.substring(1).toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    /**
     * Öffnet eine Kiste mit Auswahl-GUI statt Animation.
     * Spieler kann ein Pool-Item auswählen; garantierte Items + Bundle werden automatisch gegeben.
     */
    public void openCrateSelection(Player p, String crateId, boolean skipKeyCheck) {
        if (!crates.containsKey(crateId)) {
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cKiste nicht gefunden.");
            return;
        }
        Crate c = crates.get(crateId);
        if (!skipKeyCheck && !consumeKey(p, crateId)) {
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cDu hast keinen passenden Schlüssel.");
            return;
        }

        List<ItemStack> selectableItems = getPreviewItems(crateId);
        if (selectableItems.isEmpty()) {
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cFür diese Kiste sind keine Belohnungen konfiguriert.");
            return;
        }

        int rows = Math.max(3, Math.min(6, (selectableItems.size() + 8) / 9 + 2));
        int invSize = rows * 9;

        String tierColor = "§7";
        if (c.tier != null) {
            switch (c.tier.toLowerCase()) {
                case "legendary": tierColor = "§6"; break;
                case "rare": tierColor = "§b"; break;
                default: tierColor = "§a"; break;
            }
        }

        Inventory inv = Bukkit.createInventory(null, invSize, tierColor + "§l✦ " + c.display + " §8- §fWähle dein Gear");

        // Fill borders with glass
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName("§8⬛");
        gm.getPersistentDataContainer().set(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING, "crate_selection_border");
        glass.setItemMeta(gm);
        for (int i = 0; i < 9; i++) inv.setItem(i, glass);
        for (int i = invSize - 9; i < invSize; i++) inv.setItem(i, glass);
        for (int r = 1; r < rows - 1; r++) {
            inv.setItem(r * 9, glass);
            inv.setItem(r * 9 + 8, glass);
        }

        // Info item in top row (slot 4)
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(tierColor + "§l✦ " + c.display + " ✦");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8────────────────");
        infoLore.add("§7Wähle dein Gear!");
        infoLore.add("§8────────────────");
        infoMeta.setLore(infoLore);
        infoMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING, "crate_selection_border");
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        // Preview items in inner slots
        int slot = 10;
        for (int i = 0; i < selectableItems.size(); i++) {
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;
            if (slot >= invSize - 9) break;

            ItemStack display = selectableItems.get(i).clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("§8────────────────");
                lore.add("§a§l▶ Klicke zum Auswählen!");
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING, "crate_select_pool:" + crateId + ":" + i);
                display.setItemMeta(meta);
            }
            inv.setItem(slot, display);
            slot++;
        }

        p.openInventory(inv);
    }

    /**
     * Gibt die Belohnungen nachdem ein Spieler ein Pool-Item ausgewählt hat.
     */
    public void giveSelectionRewards(Player p, String crateId, int poolIndex) {
        Crate c = crates.get(crateId);
        if (c == null) return;
        List<ItemStack> previewItems = getPreviewItems(crateId);
        if (poolIndex < 0 || poolIndex >= previewItems.size()) return;

        // Selected preview item (already has tier enchants applied)
        ItemStack selected = previewItems.get(poolIndex).clone();
        List<ItemStack> rewards = new ArrayList<>();
        rewards.add(selected);

        // Give items
        p.closeInventory();
        StringBuilder names = new StringBuilder();
        for (ItemStack reward : rewards) {
            if (p.getInventory().firstEmpty() == -1) p.getWorld().dropItemNaturally(p.getLocation(), reward);
            else p.getInventory().addItem(reward);
            if (!names.isEmpty()) names.append(", ");
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
    }

    private ItemStack applyTierEnchantIfNeeded(ItemStack item, String tier) {
        if (item == null || tier == null) return item;
        try {
            String base = "settings.crate-tiers." + tier;
            if (!plugin.getConfig().isConfigurationSection(base)) return item;
            int maxLevel = plugin.getConfig().getInt(base + ".enchant-level", 0);
            double chance = plugin.getConfig().getDouble(base + ".enchant-chance", 0.0);
            if (maxLevel <= 0 || chance <= 0.0) return item;
            if (random.nextDouble() > chance) return item;

            // pick a random enchantment that can enchant this item
            List<org.bukkit.enchantments.Enchantment> possible = new ArrayList<>();
            for (org.bukkit.enchantments.Enchantment e : org.bukkit.enchantments.Enchantment.values()) {
                try {
                    if (e.canEnchantItem(item)) possible.add(e);
                } catch (Throwable ignored) {}
            }
            if (possible.isEmpty()) return item;
            org.bukkit.enchantments.Enchantment chosen = possible.get(random.nextInt(possible.size()));
            int lvl = 1 + random.nextInt(Math.max(1, maxLevel));
            ItemStack clone = item.clone();
            clone.addUnsafeEnchantment(chosen, lvl);
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
