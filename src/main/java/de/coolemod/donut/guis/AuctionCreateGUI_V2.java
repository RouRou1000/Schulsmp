package de.coolemod.donut.guis;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.AuctionHouseManagerNew;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Komplett neu geschriebene Auktions-Erstellungs-GUI
 * EINFACH, KLAR, BUGFREI
 */
public class AuctionCreateGUI_V2 {
    private final DonutPlugin plugin;
    private final AuctionHouseManagerNew manager;
    private final Map<UUID, Session> sessions = new HashMap<>();
    
    // PDC Keys
    private final NamespacedKey UI_KEY;
    private final NamespacedKey ACTION_KEY;
    
    public AuctionCreateGUI_V2(DonutPlugin plugin, AuctionHouseManagerNew manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.UI_KEY = new NamespacedKey(plugin, "ui_element");
        this.ACTION_KEY = new NamespacedKey(plugin, "action");
    }
    
    /**
     * Session holder
     */
    public static class Session {
        public ItemStack storedItem;  // Das echte Item (nicht im GUI)
        public double price;
        public boolean priceLocked;
        public Inventory gui;
        
        public Session() {
            this.storedItem = null;
            this.price = 0;
            this.priceLocked = false;
        }
    }
    
    /**
     * Öffne GUI für Spieler
     */
    public void open(Player player) {
        Session session = new Session();
        sessions.put(player.getUniqueId(), session);
        
        // Erstelle GUI
        session.gui = Bukkit.createInventory(null, 45, "§8ᴀᴜᴋᴛɪᴏɴ ᴇʀsᴛᴇʟʟᴇɴ");
        
        // Fülle mit Rahmen (aber nicht die Button-Slots)
        for (int i = 0; i < 45; i++) {
            // Überspringe Slots für Buttons und Item
            if (i == 20 || i == 22 || i == 24 || i == 40) continue;
            
            ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta glassMeta = glass.getItemMeta();
            glassMeta.setDisplayName(" ");
            glass.setItemMeta(glassMeta);
            session.gui.setItem(i, markUI(glass, "border"));
        }
        
        // Setze Buttons und Item
        refreshGUI(session);
        
        player.openInventory(session.gui);
    }
    
    /**
     * Aktualisiere GUI basierend auf aktuellem Status
     */
    private void refreshGUI(Session session) {
        plugin.getLogger().info("[DEBUG refreshGUI] storedItem=" + (session.storedItem != null) + " | priceLocked=" + session.priceLocked + " | price=" + session.price);
        // Slot 22: Item-Anzeige oder Placeholder
        if (session.storedItem == null) {
            // Placeholder
            ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = placeholder.getItemMeta();
            meta.setDisplayName("§7§lItem platzieren");
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7Lege ein Item aus deinem");
            lore.add("§7Inventar hier hinein");
            meta.setLore(lore);
            placeholder.setItemMeta(meta);
            session.gui.setItem(22, markUI(placeholder, "placeholder"));
        } else {
            // Zeige Item
            ItemStack display = session.storedItem.clone();
            ItemMeta meta = display.getItemMeta();
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add("§7Zu verkaufendes Item");
            if (session.priceLocked) {
                lore.add("§a§l✓ Preis gesetzt: §e$" + String.format("%.2f", session.price));
            }
            meta.setLore(lore);
            display.setItemMeta(meta);
            session.gui.setItem(22, display);
        }
        
        // Slot 20: Zurück-Button
        ItemStack backBtn = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.setDisplayName("§c§lZurück");
        List<String> backLore = new ArrayList<>();
        backLore.add("");
        backLore.add("§7Abbrechen und Item");
        backLore.add("§7zurückerhalten");
        backMeta.setLore(backLore);
        backBtn.setItemMeta(backMeta);
        session.gui.setItem(20, markUI(backBtn, "back"));
        plugin.getLogger().info("[DEBUG] Zurück-Button in Slot 20 gesetzt");
        
        // Slot 24: Preis-Button
        if (session.storedItem != null && !session.priceLocked) {
            ItemStack priceBtn = new ItemStack(Material.GOLD_INGOT);
            ItemMeta priceMeta = priceBtn.getItemMeta();
            priceMeta.setDisplayName("§6§lPreis festlegen");
            List<String> priceLore = new ArrayList<>();
            priceLore.add("");
            priceLore.add("§7Öffnet ein Schild");
            priceLore.add("§7zum Eingeben des Preises");
            priceMeta.setLore(priceLore);
            priceBtn.setItemMeta(priceMeta);
            session.gui.setItem(24, markUI(priceBtn, "setprice"));
        } else if (session.storedItem == null) {
            ItemStack priceBtn = new ItemStack(Material.GRAY_DYE);
            ItemMeta priceMeta = priceBtn.getItemMeta();
            priceMeta.setDisplayName("§7§lPreis festlegen");
            List<String> priceLore = new ArrayList<>();
            priceLore.add("");
            priceLore.add("§cZuerst Item platzieren!");
            priceMeta.setLore(priceLore);
            priceBtn.setItemMeta(priceMeta);
            session.gui.setItem(24, markUI(priceBtn, "disabled"));
        }
        
        // Slot 40: Bestätigen-Button (nur wenn Preis gesetzt)
        if (session.priceLocked) {
            ItemStack confirmBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
            ItemMeta confirmMeta = confirmBtn.getItemMeta();
            confirmMeta.setDisplayName("§a§l✓ Auktion erstellen");
            List<String> confirmLore = new ArrayList<>();
            confirmLore.add("");
            confirmLore.add("§7Item: §f" + session.storedItem.getType().name());
            confirmLore.add("§7Preis: §e$" + String.format("%.2f", session.price));
            confirmLore.add("");
            confirmLore.add("§eKlicken zum Bestätigen");
            confirmMeta.setLore(confirmLore);
            confirmBtn.setItemMeta(confirmMeta);
            session.gui.setItem(40, markUI(confirmBtn, "confirm"));
            plugin.getLogger().info("[DEBUG] Bestätigen-Button in Slot 40 gesetzt");
        } else {
            plugin.getLogger().info("[DEBUG] Bestätigen-Button NICHT gesetzt (priceLocked=false)");
        }
    }
    
    /**
     * Handle Button Click
     */
    public void handleButtonClick(Player player, String action) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        
        player.sendMessage("§7[DEBUG] Button clicked: " + action);
        
        switch (action) {
            case "back":
                // Gebe Item zurück
                if (session.storedItem != null) {
                    player.getInventory().addItem(session.storedItem);
                    player.sendMessage("§aItem zurückgegeben.");
                }
                player.closeInventory();
                sessions.remove(player.getUniqueId());
                break;
                
            case "setprice":
                if (session.storedItem == null) {
                    player.sendMessage("§cZuerst ein Item platzieren!");
                    return;
                }
                if (session.priceLocked) {
                    player.sendMessage("§cPreis bereits festgelegt!");
                    return;
                }
                // Öffne Sign GUI
                openSignGUI(player);
                break;
                
            case "confirm":
                if (!session.priceLocked || session.storedItem == null) {
                    player.sendMessage("§cBitte zuerst Item und Preis setzen!");
                    return;
                }
                // Erstelle Auktion
                String auctionId = manager.listItem(player.getUniqueId(), session.storedItem, session.price);
                if (auctionId != null) {
                    player.sendMessage("§a✓ Auktion erstellt für §e$" + String.format("%.2f", session.price));
                    session.storedItem = null; // Verhindere Dupe
                    player.closeInventory();
                    sessions.remove(player.getUniqueId());
                } else {
                    player.sendMessage("§cFehler beim Erstellen der Auktion!");
                }
                break;
        }
    }
    
    /**
     * Handle Item Placement in Slot 22
     */
    public void handleItemPlacement(Player player, ItemStack item) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        
        if (session.priceLocked) {
            player.sendMessage("§cItem ist gesperrt nach Preis-Eingabe!");
            return;
        }
        
        if (item == null || item.getType().isAir()) {
            return;
        }
        
        // Speichere Item
        session.storedItem = item.clone();
        session.storedItem.setAmount(1);
        
        // Refresh GUI
        refreshGUI(session);
        
        player.sendMessage("§aItem platziert! Setze jetzt einen Preis.");
    }
    
    /**
     * Handle Item Removal from Slot 22
     */
    public void handleItemRemoval(Player player) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        
        if (session.priceLocked) {
            player.sendMessage("§cItem kann nicht entfernt werden nach Preis-Eingabe!");
            return;
        }
        
        // Item wurde entfernt
        session.storedItem = null;
        refreshGUI(session);
    }
    
    /**
     * Öffne Sign GUI für Preis-Eingabe
     */
    private void openSignGUI(Player player) {
        try {
            org.bukkit.block.Block block = player.getLocation().add(0, 3, 0).getBlock();
            Material originalType = block.getType();
            
            block.setType(Material.OAK_SIGN);
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
            sign.setLine(0, "");
            sign.setLine(1, "^^^^^^^^^^^^^^");
            sign.setLine(2, "Preis eingeben");
            sign.setLine(3, "");
            sign.update(false, false);
            
            player.openSign(sign);
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                block.setType(originalType);
            }, 200L);
            
        } catch (Exception ex) {
            player.sendMessage("§cFehler beim Öffnen der Preis-Eingabe!");
        }
    }
    
    /**
     * Setze Preis vom Sign
     */
    public void setPrice(Player player, double price) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null) return;
        
        if (price <= 0) {
            player.sendMessage("§cPreis muss größer als 0 sein!");
            player.openInventory(session.gui);
            return;
        }
        
        if (session.storedItem == null) {
            player.sendMessage("§cKein Item platziert!");
            player.openInventory(session.gui);
            return;
        }
        
        session.price = price;
        session.priceLocked = true;
        refreshGUI(session);
        
        player.openInventory(session.gui);
        player.sendMessage("§a✓ Preis gesetzt: §e$" + String.format("%.2f", price));
        player.sendMessage("§7Klicke §a✓ Auktion erstellen §7zum Bestätigen!");
    }
    
    /**
     * Cleanup wenn GUI geschlossen wird
     */
    public void cleanup(Player player) {
        Session session = sessions.remove(player.getUniqueId());
        if (session != null && session.storedItem != null) {
            // Gebe Item nur zurück wenn nicht bestätigt
            player.getInventory().addItem(session.storedItem);
            player.sendMessage("§7Item zurückgegeben.");
        }
    }
    
    /**
     * Checke ob GUI
     */
    public boolean isCreateGUI(String title) {
        return title != null && title.contains("ᴀᴜᴋᴛɪᴏɴ ᴇʀsᴛᴇʟʟᴇɴ");
    }
    
    /**
     * Get Session
     */
    public Session getSession(UUID uuid) {
        return sessions.get(uuid);
    }
    
    /**
     * Markiere als UI Element
     */
    private ItemStack markUI(ItemStack item, String action) {
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(UI_KEY, PersistentDataType.STRING, "true");
        pdc.set(ACTION_KEY, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Checke ob UI Element
     */
    public boolean isUIElement(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(UI_KEY, PersistentDataType.STRING);
    }
    
    /**
     * Get Action von Item
     */
    public String getAction(ItemStack item) {
        if (!isUIElement(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(ACTION_KEY, PersistentDataType.STRING);
    }
}
