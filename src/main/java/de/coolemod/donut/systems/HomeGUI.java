package de.coolemod.donut.systems;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HomeGUI {
    private final DonutPlugin plugin;
    private final HomeManager homeManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey homeNameKey;

    public static final int MAX_HOMES = 5;
    public static final String MAIN_TITLE = "§8✦ §6§lHomes §8• §7Übersicht";
    public static final String DELETE_TITLE = "§8✦ §c§lHome löschen §8• §7Bestätigen";

    private static final int[] HOME_SLOTS = {20, 22, 24, 30, 32};

    public HomeGUI(DonutPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.actionKey = new NamespacedKey(plugin, "home_action");
        this.homeNameKey = new NamespacedKey(plugin, "home_name");
    }

    public Inventory createHomesGUI(Player player) {
        HomesViewHolder holder = new HomesViewHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, MAIN_TITLE);
        holder.inventory = inv;

        List<String> homeNames = homeManager.getHomeNames(player).stream()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        Map<String, Location> homeLocs = homeManager.getHomesMap(player);

        fillMainLayout(inv);
        inv.setItem(4, createHeaderItem(homeNames.size()));
        inv.setItem(13, createProfileItem(player, homeNames.size()));
        inv.setItem(37, createCapacityItem(homeNames.size()));
        inv.setItem(40, createCreateItem(homeNames.size()));
        inv.setItem(43, createGuideItem());
        inv.setItem(49, createCloseItem());

        for (int i = 0; i < HOME_SLOTS.length; i++) {
            if (i < homeNames.size()) {
                String homeName = homeNames.get(i);
                inv.setItem(HOME_SLOTS[i], createHomeItem(homeName, homeLocs.get(homeName), i + 1));
            } else {
                inv.setItem(HOME_SLOTS[i], createEmptySlotItem(i + 1, homeNames.size() < MAX_HOMES));
            }
        }

        return inv;
    }

    public Inventory createDeleteConfirmGUI(String homeName) {
        DeleteViewHolder holder = new DeleteViewHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, DELETE_TITLE);
        holder.inventory = inv;

        ItemStack dark = createStaticItem(Material.BLACK_STAINED_GLASS_PANE, "§0");
        ItemStack accent = createStaticItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, dark);
        }
        int[] accentSlots = {10, 12, 14, 16};
        for (int slot : accentSlots) {
            inv.setItem(slot, accent);
        }

        inv.setItem(11, createActionItem(Material.LIME_WOOL, "§a§lJa, löschen",
            List.of(
                "",
                "§7Löscht das Home §f" + homeName + "§7 endgültig.",
                "",
                "§aKlicken zum Bestätigen"
            ),
            "confirm_delete",
            homeName
        ));

        inv.setItem(13, createActionItem(Material.RECOVERY_COMPASS, "§c§l⌂ §f" + homeName.toUpperCase(),
            List.of(
                "",
                "§7Diese Aktion kann nicht rückgängig",
                "§7gemacht werden.",
                "",
                "§8Nur fortfahren, wenn du sicher bist."
            ),
            "border",
            homeName
        ));

        inv.setItem(15, createActionItem(Material.RED_WOOL, "§c§lAbbrechen",
            List.of(
                "",
                "§7Zurück zur Homes-Übersicht.",
                "",
                "§cKlicken zum Zurückkehren"
            ),
            "cancel_delete",
            homeName
        ));

        return inv;
    }

    public boolean isHomeInventory(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder();
        return holder instanceof HomesViewHolder || holder instanceof DeleteViewHolder;
    }

    public String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    public String getHomeName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(homeNameKey, PersistentDataType.STRING);
    }

    private void fillMainLayout(Inventory inv) {
        ItemStack dark = createStaticItem(Material.BLACK_STAINED_GLASS_PANE, "§0");
        ItemStack soft = createStaticItem(Material.GRAY_STAINED_GLASS_PANE, "§8");
        ItemStack accent = createStaticItem(Material.ORANGE_STAINED_GLASS_PANE, "§6");

        for (int slot = 0; slot < inv.getSize(); slot++) {
            inv.setItem(slot, dark);
        }

        int[] softSlots = {10, 11, 12, 14, 15, 16, 28, 29, 33, 34, 38, 39, 41, 42};
        for (int slot : softSlots) {
            inv.setItem(slot, soft);
        }

        int[] accentSlots = {3, 5, 19, 21, 23, 25, 31};
        for (int slot : accentSlots) {
            inv.setItem(slot, accent);
        }
    }

    private ItemStack createHeaderItem(int homeCount) {
        return createActionItem(Material.RECOVERY_COMPASS, "§6§lHOME TERMINAL",
            List.of(
                "",
                "§7Verwalte deine Homes in einer",
                "§7zentralen Übersicht.",
                "",
                "§8• §7Belegt: §6" + homeCount + "§7/§f" + MAX_HOMES,
                "§8• §7Linksklick auf ein Home §8→ §fTeleport",
                "§8• §7Rechtsklick auf ein Home §8→ §fLöschen"
            ),
            "border",
            null
        );
    }

    private ItemStack createProfileItem(Player player, int homeCount) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            meta = skullMeta;
        }
        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§f§l" + player.getName());
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§8• §7Homes gespeichert: §6" + homeCount);
        lore.add("§8• §7Freie Slots: §a" + Math.max(0, MAX_HOMES - homeCount));
        lore.add("");
        lore.add("§7Nutze §e/homes§7 oder §e/home list");
        lore.add("§7für diese Übersicht.");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "border");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCapacityItem(int homeCount) {
        return createActionItem(Material.BOOK, "§e§lKapazität",
            List.of(
                "",
                "§8• §7Aktuell belegt: §6" + homeCount + "§7/§f" + MAX_HOMES,
                "§8• §7Verfügbar: §a" + Math.max(0, MAX_HOMES - homeCount) + " Slots",
                "",
                homeCount >= MAX_HOMES
                    ? "§cLösche ein Home, um ein neues anzulegen."
                    : "§aDu kannst noch weitere Homes erstellen."
            ),
            "border",
            null
        );
    }

    private ItemStack createCreateItem(int homeCount) {
        boolean hasSpace = homeCount < MAX_HOMES;
        Material material = hasSpace ? Material.LIME_DYE : Material.RED_DYE;
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§8• §7Homes: §6" + homeCount + "§7/§f" + MAX_HOMES);
        lore.add("");
        if (hasSpace) {
            lore.add("§7Lege einen neuen Homepunkt an");
            lore.add("§7und benenne ihn direkt per Schild.");
            lore.add("");
            lore.add("§aKlicken zum Erstellen");
        } else {
            lore.add("§cDu hast das Maximum erreicht.");
            lore.add("§7Lösche zuerst einen bestehenden Homepunkt.");
        }
        return createActionItem(material, hasSpace ? "§a§lNeues Home" : "§c§lKein Slot frei", lore, hasSpace ? "create" : "border", null);
    }

    private ItemStack createGuideItem() {
        return createActionItem(Material.NAME_TAG, "§b§lSteuerung",
            List.of(
                "",
                "§aLinksklick §8→ §7Teleportieren",
                "§cRechtsklick §8→ §7Löschen",
                "§e/home set <name> §8→ §7Schnell setzen",
                "§e/home del <name> §8→ §7Direkt löschen"
            ),
            "border",
            null
        );
    }

    private ItemStack createCloseItem() {
        return createActionItem(Material.BARRIER, "§c§lSchließen",
            List.of("", "§7Schließt diese Übersicht."),
            "close",
            null
        );
    }

    private ItemStack createHomeItem(String name, Location loc, int index) {
        ItemStack item = new ItemStack(getHomeMaterial(loc));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§6§l⌂ §f" + name.toUpperCase());
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§8• §7Slot: §6#" + index);
        if (loc != null && loc.getWorld() != null) {
            lore.add("§8• §7Welt: §f" + formatWorldName(loc.getWorld()));
            lore.add("§8• §7Position: §f" + loc.getBlockX() + "§8, §f" + loc.getBlockY() + "§8, §f" + loc.getBlockZ());
        }
        lore.add("");
        lore.add("§aLinksklick §8→ §7Teleportieren");
        lore.add("§cRechtsklick §8→ §7Löschen");
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "home");
        meta.getPersistentDataContainer().set(homeNameKey, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptySlotItem(int index, boolean creatable) {
        Material material = creatable ? Material.GRAY_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§8• §7Slot: §6#" + index);
        lore.add(creatable ? "§7Dieser Platz ist noch frei." : "§7Zurzeit kann kein weiterer Homepunkt angelegt werden.");
        lore.add("");
        lore.add(creatable ? "§7Nutze unten §aNeues Home§7 zum Erstellen." : "§cLösche ein bestehendes Home, um Platz zu schaffen.");
        return createActionItem(material, creatable ? "§7Freier Slot" : "§cSlot gesperrt", lore, "border", null);
    }

    private ItemStack createStaticItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(displayName);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "border");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionItem(Material material, String displayName, List<String> lore, String action, String homeName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        if (homeName != null) {
            meta.getPersistentDataContainer().set(homeNameKey, PersistentDataType.STRING, homeName);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Material getHomeMaterial(Location location) {
        if (location == null || location.getWorld() == null) {
            return Material.RECOVERY_COMPASS;
        }
        return switch (location.getWorld().getEnvironment()) {
            case NETHER -> Material.NETHERRACK;
            case THE_END -> Material.END_STONE;
            default -> Material.GRASS_BLOCK;
        };
    }

    private String formatWorldName(World world) {
        return switch (world.getEnvironment()) {
            case NETHER -> "Nether";
            case THE_END -> "End";
            default -> "Overworld";
        };
    }

    private static final class HomesViewHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class DeleteViewHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
