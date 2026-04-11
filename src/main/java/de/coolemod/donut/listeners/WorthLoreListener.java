package de.coolemod.donut.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.SellMultiplierManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Fügt Worth-Lore nur clientseitig über ProtocolLib hinzu.
 * Die echten Server-Items bleiben unverändert und damit stackbar.
 */
public class WorthLoreListener implements Listener {
    private final DonutPlugin plugin;
    private final ProtocolManager protocolManager;
    private static final String WORTH_PREFIX = "§7Wert pro Stück: §a$";

    public WorthLoreListener(DonutPlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        protocolManager.addPacketListener(new PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.SET_SLOT,
                PacketType.Play.Server.WINDOW_ITEMS
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;
                handleOutgoingInventoryPacket(event);
            }
        });

        // Periodisch alte Worth-Lore von echten Items entfernen (falls welche durchgesickert sind)
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                cleanOldWorthLore(player);
            }
        }, 20L, 200L);
    }

    private void handleOutgoingInventoryPacket(PacketEvent event) {
        PacketContainer packet = event.getPacket().deepClone();
        Player player = event.getPlayer();

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            ItemStack item = packet.getItemModifier().readSafely(0);
            packet.getItemModifier().writeSafely(0, withClientWorthLore(item, player));
            event.setPacket(packet);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            List<ItemStack> items = packet.getItemListModifier().readSafely(0);
            if (items != null) {
                List<ItemStack> updated = new ArrayList<>(items.size());
                for (ItemStack item : items) {
                    updated.add(withClientWorthLore(item, player));
                }
                packet.getItemListModifier().writeSafely(0, updated);
            }

            if (packet.getItemModifier().size() > 0) {
                ItemStack carried = packet.getItemModifier().readSafely(0);
                packet.getItemModifier().writeSafely(0, withClientWorthLore(carried, player));
            }

            event.setPacket(packet);
        }
    }

    private ItemStack withClientWorthLore(ItemStack original, Player viewer) {
        if (original == null || original.getType() == Material.AIR || !original.getType().isItem()) {
            return original;
        }

        if (isGUIDisplayItem(original)) {
            return original;
        }

        // Shulker Box: zeige Gesamtwert des Inhalts
        if (original.getType().name().endsWith("SHULKER_BOX")) {
            return withShulkerWorthLore(original, viewer);
        }

        double worthPer = plugin.getWorthManager().getWorth(original);
        if (worthPer <= 0) {
            return original;
        }

        // Sell Multiplier
        double multi = plugin.getSellMultiplier().getMultiplier(viewer.getUniqueId(), original.getType());
        double effectiveWorth = worthPer * multi;

        ItemStack clientCopy = original.clone();
        ItemMeta meta = clientCopy.getItemMeta();
        if (meta == null) {
            return original;
        }

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> line.startsWith("§7Wert:") || line.startsWith("§7Wert pro Stück:") || line.startsWith("§7Gesamt:") || line.startsWith("§7Multi:"));

        double totalWorth = effectiveWorth * clientCopy.getAmount();
        if (multi > 1.0) {
            lore.add(WORTH_PREFIX + de.coolemod.donut.utils.NumberFormatter.format(effectiveWorth)
                    + " §8(§e" + SellMultiplierManager.formatMultiplier(multi) + "§8)");
        } else {
            lore.add(WORTH_PREFIX + de.coolemod.donut.utils.NumberFormatter.format(effectiveWorth));
        }
        // Verzauberungs-Bonus anzeigen
        double basePer = plugin.getWorthManager().getWorth(original.getType());
        double enchBonus = worthPer - basePer;
        if (enchBonus > 0.01) {
            lore.add("§8  §5⚈ Verzauberungen§8: §a+" + de.coolemod.donut.utils.NumberFormatter.format(enchBonus * multi) + "$");
        }
        if (clientCopy.getAmount() > 1) {
            lore.add("§7Gesamt: §a" + de.coolemod.donut.utils.NumberFormatter.formatMoney(totalWorth));
        }

        meta.setLore(lore);
        clientCopy.setItemMeta(meta);
        return clientCopy;
    }

    private boolean isGUIDisplayItem(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_cost_money"), PersistentDataType.INTEGER)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_cost_shards"), PersistentDataType.INTEGER)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_category"), PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_back"), PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "donut_gui_action"), PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "order_ui"), PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "worth_page"), PersistentDataType.INTEGER)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "worth_tab"), PersistentDataType.STRING);
    }

    private ItemStack withShulkerWorthLore(ItemStack original, Player viewer) {
        if (!(original.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta blockMeta)) return original;
        if (!(blockMeta.getBlockState() instanceof org.bukkit.block.ShulkerBox box)) return original;

        org.bukkit.inventory.Inventory shulkerInv = box.getInventory();
        double total = 0;
        int items = 0;
        for (int i = 0; i < shulkerInv.getSize(); i++) {
            ItemStack si = shulkerInv.getItem(i);
            if (si == null || si.getType() == Material.AIR) continue;
            double worthPer = plugin.getWorthManager().getWorth(si);
            if (worthPer <= 0) continue;
            double multi = plugin.getSellMultiplier().getMultiplier(viewer.getUniqueId(), si.getType());
            total += worthPer * multi * si.getAmount();
            items += si.getAmount();
        }

        if (total <= 0) return original;

        ItemStack clientCopy = original.clone();
        ItemMeta meta = clientCopy.getItemMeta();
        if (meta == null) return original;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> line.startsWith("§7Inhalt:") || line.startsWith("§7Gesamtwert:"));
        lore.add("§7Inhalt: §f" + items + " Items");
        lore.add("§7Gesamtwert: §a" + de.coolemod.donut.utils.NumberFormatter.formatMoney(total));

        meta.setLore(lore);
        clientCopy.setItemMeta(meta);
        return clientCopy;
    }

    private void cleanOldWorthLore(Player player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>(meta.getLore());
            boolean changed = lore.removeIf(line -> line.startsWith("§7Wert:") || line.startsWith("§7Wert pro Stück:") || line.startsWith("§7Gesamt:"));
            if (!changed) {
                continue;
            }
            meta.setLore(lore.isEmpty() ? null : lore);
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> cleanOldWorthLore(e.getPlayer()), 5L);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            refreshClientInventory(player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player player) {
            refreshClientInventory(player);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player player) {
            refreshClientInventory(player);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        refreshClientInventory(e.getPlayer());
    }

    @EventHandler
    public void onSlotSwitch(PlayerItemHeldEvent e) {
        refreshClientInventory(e.getPlayer());
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent e) {
        refreshClientInventory(e.getPlayer());
    }

    private void refreshClientInventory(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        }, 1L);
    }
}
