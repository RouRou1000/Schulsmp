package de.coolemod.donut.systems;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HomeGUI {
    private final DonutPlugin plugin;
    private final HomeManager homeManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey homeNameKey;
    
    public static final int MAX_HOMES = 5;
    
    public HomeGUI(DonutPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
        this.actionKey = new NamespacedKey(plugin, "home_action");
        this.homeNameKey = new NamespacedKey(plugin, "home_name");
    }
    
    private String toSmallCaps(String text) {
        return text.toUpperCase()
            .replace("A", "ᴀ").replace("B", "ʙ").replace("C", "ᴄ")
            .replace("D", "ᴅ").replace("E", "ᴇ").replace("F", "ғ")
            .replace("G", "ɢ").replace("H", "ʜ").replace("I", "ɪ")
            .replace("J", "ᴊ").replace("K", "ᴋ").replace("L", "ʟ")
            .replace("M", "ᴍ").replace("N", "ɴ").replace("O", "ᴏ")
            .replace("P", "ᴘ").replace("Q", "ǫ").replace("R", "ʀ")
            .replace("S", "s").replace("T", "ᴛ").replace("U", "ᴜ")
            .replace("V", "ᴠ").replace("W", "ᴡ").replace("X", "x")
            .replace("Y", "ʏ").replace("Z", "ᴢ");
    }
    
    public Inventory createHomesGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8● §6§lHomes §8●");
        
        List<String> homeNames = homeManager.getHomeNames(player);
        Map<String, Location> homeLocs = homeManager.getHomesMap(player);
        
        // Fill border with black glass
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("§0");
        borderMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "border");
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }
        
        // Show homes in slots 10-14 (middle row)
        int slot = 10;
        for (String homeName : homeNames) {
            if (slot > 14) break;
            
            Location loc = homeLocs.get(homeName);
            ItemStack homeItem = createHomeItem(homeName, loc);
            inv.setItem(slot, homeItem);
            slot++;
        }
        
        // Fill remaining home slots with "create new" if under limit
        while (slot <= 14) {
            if (homeNames.size() < MAX_HOMES) {
                ItemStack create = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
                ItemMeta createMeta = create.getItemMeta();
                createMeta.setDisplayName("§a§l+ §fNeues Home");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§8▸ §7Homes: §a" + homeNames.size() + "§7/§f" + MAX_HOMES);
                lore.add("");
                lore.add("§aKlicken zum Erstellen");
                createMeta.setLore(lore);
                createMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "create");
                create.setItemMeta(createMeta);
                inv.setItem(slot, create);
            } else {
                ItemStack full = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                ItemMeta fullMeta = full.getItemMeta();
                fullMeta.setDisplayName("§c§l✖ §fMaximum");
                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add("§8▸ §7Alle §c" + MAX_HOMES + " §7Slots belegt");
                lore.add("§8▸ §7Lösche ein Home");
                fullMeta.setLore(lore);
                fullMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "border");
                full.setItemMeta(fullMeta);
                inv.setItem(slot, full);
            }
            slot++;
        }
        
        // Info item (slot 4)
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§lInfo");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("§8▸ §7Homes: §a" + homeNames.size() + "§7/§f" + MAX_HOMES);
        infoLore.add("");
        infoLore.add("§e⚡ §7Linksklick = Teleport");
        infoLore.add("§c✖ §7Rechtsklick = Löschen");
        infoMeta.setLore(infoLore);
        infoMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "border");
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);
        
        // Close button (slot 22)
        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = close.getItemMeta();
        closeMeta.setDisplayName("§c§lSchließen");
        closeMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "close");
        close.setItemMeta(closeMeta);
        inv.setItem(22, close);
        
        return inv;
    }
    
    private ItemStack createHomeItem(String name, Location loc) {
        ItemStack item = new ItemStack(Material.CYAN_BED);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l" + name.toUpperCase());
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (loc != null && loc.getWorld() != null) {
            lore.add("§8▸ §7Welt: §f" + loc.getWorld().getName());
            lore.add("§8▸ §7X: §a" + (int)loc.getX() + " §7Y: §e" + (int)loc.getY() + " §7Z: §a" + (int)loc.getZ());
        }
        lore.add("");
        lore.add("§aLinksklick §8→ §7Teleport");
        lore.add("§cRechtsklick §8→ §7Löschen");
        meta.setLore(lore);
        
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "home");
        meta.getPersistentDataContainer().set(homeNameKey, PersistentDataType.STRING, name);
        item.setItemMeta(meta);
        
        return item;
    }
    
    public Inventory createDeleteConfirmGUI(String homeName) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8● §c§lHome Löschen? §8●");
        
        // Fill with black glass
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("§0");
        borderMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "border");
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }
        
        // Confirm delete (slot 11)
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lJa, Löschen");
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add("");
        confirmLore.add("§8▸ §7Home §f" + homeName + " §7wird");
        confirmLore.add("§7unwiderruflich gelöscht!");
        confirmLore.add("§8");
        confirmLore.add("§a▸ Klicken zum Bestätigen");
        confirmMeta.setLore(confirmLore);
        confirmMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "confirm_delete");
        confirmMeta.getPersistentDataContainer().set(homeNameKey, PersistentDataType.STRING, homeName);
        confirm.setItemMeta(confirmMeta);
        inv.setItem(11, confirm);
        
        // Info item (slot 13)
        ItemStack info = new ItemStack(Material.CYAN_BED);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§c§l🏠 §f" + homeName.toUpperCase());
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("§8▸ §7Dieses Home wird");
        infoLore.add("§7  unwiderruflich gelöscht!");
        infoMeta.setLore(infoLore);
        infoMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "border");
        info.setItemMeta(infoMeta);
        inv.setItem(13, info);
        
        // Cancel (slot 15)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§lAbbrechen");
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("");
        cancelLore.add("§8▸ §7Zurück zur Home-Übersicht");
        cancelLore.add("");
        cancelLore.add("§c✖ §7Klicken zum Abbrechen");
        cancelMeta.setLore(cancelLore);
        cancelMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "cancel_delete");
        cancel.setItemMeta(cancelMeta);
        inv.setItem(15, cancel);
        
        return inv;
    }
    
    public String getAction(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }
    
    public String getHomeName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(homeNameKey, PersistentDataType.STRING);
    }
}
