package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.OrdersManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class OrdersGUI {
    private final DonutPlugin plugin;
    public OrdersGUI(DonutPlugin plugin) { this.plugin = plugin; }

    public void open(Player p) { open(p, 1); }

    public void open(Player p, int page) {
        List<OrdersManager.Order> orders = new ArrayList<>(plugin.getOrdersManager().getAll());
        int pageSize = 45;
        int totalPages = Math.max(1, (orders.size() + pageSize - 1) / pageSize);
        page = Math.max(1, Math.min(page, totalPages));

        Inventory inv = GUIUtils.createMenu("Orders - Seite " + page + "/" + totalPages, 6);
        GUIUtils.fillBorders(inv, plugin);

        int start = (page - 1) * pageSize;
        int idx = 0;
        for (int i = start; i < Math.min(orders.size(), start + pageSize); i++) {
            OrdersManager.Order o = orders.get(i);
            ItemStack is = o.itemType.clone();
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.addFirst("§7Benötigt: §a" + o.requiredAmount + " (geliefert: " + o.delivered + ")");
                lore.add(1, "§7Preis/Stück: §a" + "%.2f".formatted(o.pricePerItem));
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "order_id"), org.bukkit.persistence.PersistentDataType.STRING, o.id);
                meta.setLore(lore);
                is.setItemMeta(meta);
            }
            is.setAmount(1);
            inv.setItem(idx++, is);
        }

        if (page > 1) inv.setItem(45, GUIUtils.navItem(plugin, Material.ARROW, "§eZurück", "orders_prev", page - 1));
        inv.setItem(48, GUIUtils.navItem(plugin, Material.EMERALD, "§a+ Order erstellen", "orders_create", 0));
        inv.setItem(49, GUIUtils.navItem(plugin, Material.BARRIER, "§cSchließen", "orders_close", page));
        inv.setItem(50, GUIUtils.navItem(plugin, Material.CHEST, "§eMeine Orders", "orders_my", 0));
        if (page < totalPages) inv.setItem(53, GUIUtils.navItem(plugin, Material.ARROW, "§eWeiter", "orders_next", page + 1));

        GUIUtils.open(p, inv);
    }

    public void openMyOrders(Player p) {
        List<OrdersManager.Order> myOrders = plugin.getOrdersManager().getByOwner(p.getUniqueId());
        Inventory inv = GUIUtils.createMenu("§e✦ Meine Orders ✦", 6);
        GUIUtils.fillBorders(inv, plugin);

        ItemStack info = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta im = info.getItemMeta();
        im.setDisplayName("§6Meine Orders");
        List<String> il = new ArrayList<>();
        il.add("§7Deine Orders: §e" + myOrders.size());
        il.add("§7Rechtsklick zum Stornieren");
        im.setLore(il);
        info.setItemMeta(im);
        inv.setItem(4, info);

        int slot = 10;
        for (OrdersManager.Order o : myOrders) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
            ItemStack is = o.itemType.clone();
            ItemMeta meta = is.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.addFirst("§8──────────────");
                lore.add(1, "§7Benötigt: §a" + o.requiredAmount);
                lore.add(2, "§7Geliefert: §e" + o.delivered);
                lore.add(3, "§7Preis/Stück: §a$" + "%.2f".formatted(o.pricePerItem));
                lore.add(4, "§8──────────────");
                lore.add(5, "§c➤ Rechtsklick zum Stornieren");
                meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "order_cancel"), org.bukkit.persistence.PersistentDataType.STRING, o.id);
                meta.setLore(lore);
                is.setItemMeta(meta);
            }
            inv.setItem(slot++, is);
        }

        inv.setItem(49, GUIUtils.navItem(plugin, Material.ARROW, "§eZurück zu Orders", "orders_back", 1));
        GUIUtils.open(p, inv);
    }
}
