package de.coolemod.donut.systems;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public enum SpawnerType {
    ZOMBIE(EntityType.ZOMBIE, "§2Zombie Spawner", Material.ROTTEN_FLESH,
        new Drop(Material.ROTTEN_FLESH, 1, 2, 100), new Drop(Material.IRON_INGOT, 1, 1, 5),
        new Drop(Material.CARROT, 1, 1, 3), new Drop(Material.POTATO, 1, 1, 3)),
    SKELETON(EntityType.SKELETON, "§fSkeleton Spawner", Material.BONE,
        new Drop(Material.BONE, 1, 2, 100), new Drop(Material.ARROW, 1, 3, 80)),
    CREEPER(EntityType.CREEPER, "§aCreeper Spawner", Material.GUNPOWDER,
        new Drop(Material.GUNPOWDER, 1, 2, 100)),
    SPIDER(EntityType.SPIDER, "§4Spider Spawner", Material.STRING,
        new Drop(Material.STRING, 1, 2, 100), new Drop(Material.SPIDER_EYE, 1, 1, 50)),
    CAVE_SPIDER(EntityType.CAVE_SPIDER, "§2Cave Spider Spawner", Material.FERMENTED_SPIDER_EYE,
        new Drop(Material.STRING, 1, 2, 100), new Drop(Material.SPIDER_EYE, 1, 2, 75)),
    ENDERMAN(EntityType.ENDERMAN, "§5Enderman Spawner", Material.ENDER_PEARL,
        new Drop(Material.ENDER_PEARL, 1, 1, 100)),
    BLAZE(EntityType.BLAZE, "§6Blaze Spawner", Material.BLAZE_ROD,
        new Drop(Material.BLAZE_ROD, 1, 2, 100)),
    COW(EntityType.COW, "§fCow Spawner", Material.LEATHER,
        new Drop(Material.BEEF, 1, 3, 100), new Drop(Material.LEATHER, 1, 2, 100)),
    PIG(EntityType.PIG, "§dPig Spawner", Material.PORKCHOP,
        new Drop(Material.PORKCHOP, 1, 3, 100)),
    SHEEP(EntityType.SHEEP, "§fSheep Spawner", Material.WHITE_WOOL,
        new Drop(Material.MUTTON, 1, 2, 100), new Drop(Material.WHITE_WOOL, 1, 1, 100)),
    CHICKEN(EntityType.CHICKEN, "§eChicken Spawner", Material.FEATHER,
        new Drop(Material.CHICKEN, 1, 1, 100), new Drop(Material.FEATHER, 1, 2, 100)),
    IRON_GOLEM(EntityType.IRON_GOLEM, "§7§lIron Golem Spawner", Material.IRON_INGOT,
        new Drop(Material.IRON_INGOT, 3, 5, 100), new Drop(Material.POPPY, 1, 2, 50)),
    WITCH(EntityType.WITCH, "§5Witch Spawner", Material.GLASS_BOTTLE,
        new Drop(Material.GLASS_BOTTLE, 1, 2, 40), new Drop(Material.GLOWSTONE_DUST, 1, 2, 30),
        new Drop(Material.REDSTONE, 1, 2, 30), new Drop(Material.SPIDER_EYE, 1, 1, 20),
        new Drop(Material.SUGAR, 1, 2, 20), new Drop(Material.GUNPOWDER, 1, 2, 25),
        new Drop(Material.STICK, 1, 2, 25)),
    SLIME(EntityType.SLIME, "§aSlime Spawner", Material.SLIME_BALL,
        new Drop(Material.SLIME_BALL, 1, 2, 100)),
    MAGMA_CUBE(EntityType.MAGMA_CUBE, "§6Magma Cube Spawner", Material.MAGMA_CREAM,
        new Drop(Material.MAGMA_CREAM, 1, 2, 100)),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, "§0Wither Skeleton Spawner", Material.COAL,
        new Drop(Material.COAL, 1, 1, 100), new Drop(Material.BONE, 1, 2, 100),
        new Drop(Material.WITHER_SKELETON_SKULL, 1, 1, 3)),
    PHANTOM(EntityType.PHANTOM, "§9Phantom Spawner", Material.PHANTOM_MEMBRANE,
        new Drop(Material.PHANTOM_MEMBRANE, 1, 2, 100)),
    DROWNED(EntityType.DROWNED, "§3Drowned Spawner", Material.ROTTEN_FLESH,
        new Drop(Material.ROTTEN_FLESH, 1, 2, 100), new Drop(Material.GOLD_INGOT, 1, 1, 10),
        new Drop(Material.COPPER_INGOT, 1, 2, 15), new Drop(Material.TRIDENT, 1, 1, 2)),
    GUARDIAN(EntityType.GUARDIAN, "§bGuardian Spawner", Material.PRISMARINE_SHARD,
        new Drop(Material.PRISMARINE_SHARD, 1, 2, 100), new Drop(Material.PRISMARINE_CRYSTALS, 1, 1, 50),
        new Drop(Material.COD, 1, 1, 40));

    private final EntityType entityType;
    private final String displayName;
    private final Material icon;
    private final List<Drop> drops;
    private static final Random random = new Random();

    SpawnerType(EntityType entityType, String displayName, Material icon, Drop... drops) {
        this.entityType = entityType;
        this.displayName = displayName;
        this.icon = icon;
        this.drops = Arrays.asList(drops);
    }

    public EntityType getEntityType() { return entityType; }
    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }

    public List<ItemStack> generateDrops(int stackSize) {
        Map<Material, Integer> dropMap = new HashMap<>();
        for (int i = 0; i < stackSize; i++) {
            for (Drop drop : drops) {
                if (random.nextInt(100) < drop.chance) {
                    int amount = drop.minAmount + random.nextInt(drop.maxAmount - drop.minAmount + 1);
                    dropMap.merge(drop.material, amount, Integer::sum);
                }
            }
        }
        List<ItemStack> result = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : dropMap.entrySet()) {
            int total = entry.getValue();
            while (total > 0) {
                int stackAmount = Math.min(total, 64);
                result.add(new ItemStack(entry.getKey(), stackAmount));
                total -= stackAmount;
            }
        }
        return result;
    }

    public static SpawnerType fromEntityType(EntityType type) {
        for (SpawnerType st : values()) { if (st.entityType == type) return st; }
        return null;
    }

    public static SpawnerType fromName(String name) {
        try { return valueOf(name.toUpperCase()); } catch (IllegalArgumentException e) { return null; }
    }

    public static class Drop {
        public final Material material;
        public final int minAmount;
        public final int maxAmount;
        public final int chance;

        public Drop(Material material, int minAmount, int maxAmount, int chance) {
            this.material = material;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.chance = chance;
        }
    }
}
