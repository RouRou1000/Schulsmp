package de.coolemod.donut.systems;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlacedSpawner {
    private final Location location;
    private final SpawnerType type;
    private int stackSize;
    private final UUID owner;
    private long lastDropTime;
    private final List<ItemStack> storedDrops = new ArrayList<>();

    private static int BASE_DROP_CAP = 1000;
    private static int CAP_PER_STACK = 500;

    public PlacedSpawner(Location location, SpawnerType type, int stackSize, UUID owner, long lastDropTime) {
        this.location = location;
        this.type = type;
        this.stackSize = stackSize;
        this.owner = owner;
        this.lastDropTime = lastDropTime;
    }

    public static void setCapConfig(int baseDropCap, int capPerStack) {
        BASE_DROP_CAP = baseDropCap;
        CAP_PER_STACK = capPerStack;
    }

    public int getMaxStoredDrops() { return BASE_DROP_CAP + (stackSize * CAP_PER_STACK); }

    public Location getLocation() { return location; }
    public SpawnerType getType() { return type; }
    public int getStackSize() { return stackSize; }
    public UUID getOwner() { return owner; }
    public long getLastDropTime() { return lastDropTime; }
    public void setStackSize(int stackSize) { this.stackSize = stackSize; }
    public void setLastDropTime(long lastDropTime) { this.lastDropTime = lastDropTime; }

    public void addDrops(List<ItemStack> drops) {
        int currentCount = getStoredDropCount();
        int maxDrops = getMaxStoredDrops();
        if (currentCount >= maxDrops) return;
        int spaceLeft = maxDrops - currentCount;

        for (ItemStack newDrop : drops) {
            if (spaceLeft <= 0) break;
            boolean merged = false;
            for (ItemStack stored : storedDrops) {
                if (stored.isSimilar(newDrop) && stored.getAmount() < 64) {
                    int canAdd = Math.min(64 - stored.getAmount(), spaceLeft);
                    int toAdd = Math.min(canAdd, newDrop.getAmount());
                    stored.setAmount(stored.getAmount() + toAdd);
                    newDrop.setAmount(newDrop.getAmount() - toAdd);
                    spaceLeft -= toAdd;
                    if (newDrop.getAmount() <= 0) { merged = true; break; }
                }
            }
            if (!merged && newDrop.getAmount() > 0 && spaceLeft > 0) {
                int toAdd = Math.min(newDrop.getAmount(), spaceLeft);
                ItemStack toStore = newDrop.clone();
                toStore.setAmount(toAdd);
                storedDrops.add(toStore);
                spaceLeft -= toAdd;
            }
        }
    }

    public List<ItemStack> collectDrops() {
        List<ItemStack> result = new ArrayList<>(storedDrops);
        storedDrops.clear();
        return result;
    }

    public int getStoredDropCount() { return storedDrops.stream().mapToInt(ItemStack::getAmount).sum(); }

    public List<ItemStack> getStoredDrops() {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : storedDrops) result.add(item.clone());
        return result;
    }

    public String getDropsSummary() {
        if (storedDrops.isEmpty()) return "§7Keine Items";
        int total = storedDrops.stream().mapToInt(ItemStack::getAmount).sum();
        return "§e" + total + " §7Items bereit";
    }
}
