package de.coolemod.schulcore.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.coolemod.schulcore.SchulCorePlugin;
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
import org.bukkit.GameMode;
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
    private final SchulCorePlugin plugin;
    private final ProtocolManager protocolManager;
    private final PacketAdapter packetListener;
    private static final String WORTH_PREFIX = "§7Wert pro Stück: §a$";

    public WorthLoreListener(SchulCorePlugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.packetListener = new PacketAdapter(
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
        };

        protocolManager.addPacketListener(packetListener);

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                cleanOldWorthLore(player);
            }
        }, 20L, 200L);
    }

    private void handleOutgoingInventoryPacket(PacketEvent event) {
        PacketContainer packet = event.getPacket().deepClone();

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            ItemStack item = packet.getItemModifier().readSafely(0);
            packet.getItemModifier().writeSafely(0, withClientWorthLore(item));
            event.setPacket(packet);
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            List<ItemStack> items = packet.getItemListModifier().readSafely(0);
            if (items != null) {
                List<ItemStack> updated = new ArrayList<>(items.size());
                for (ItemStack item : items) {
                    updated.add(withClientWorthLore(item));
                }
                packet.getItemListModifier().writeSafely(0, updated);
            }

            if (packet.getItemModifier().size() > 0) {
                ItemStack carried = packet.getItemModifier().readSafely(0);
                packet.getItemModifier().writeSafely(0, withClientWorthLore(carried));
            }

            event.setPacket(packet);
        }
    }

    private ItemStack withClientWorthLore(ItemStack original) {
        if (original == null || original.getType() == Material.AIR || !original.getType().isItem()) {
            return original;
        }

        if (isShopDisplayItem(original)) {
            return original;
        }

        double worthPer = plugin.getWorthManager().getWorth(original);
        if (worthPer <= 0) {
            return original;
        }

        ItemStack clientCopy = original.clone();
        ItemMeta meta = clientCopy.getItemMeta();
        if (meta == null) {
            return original;
        }

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.removeIf(line -> line.startsWith("§7Wert:" ) || line.startsWith("§7Wert pro Stück:"));

        double totalWorth = worthPer * clientCopy.getAmount();
        lore.add(WORTH_PREFIX + String.format("%.0f", worthPer));
        if (clientCopy.getAmount() > 1) {
            lore.add("§7Gesamt: §a$" + String.format("%.0f", totalWorth));
        }

        meta.setLore(lore);
        clientCopy.setItemMeta(meta);
        return clientCopy;
    }

    private boolean isShopDisplayItem(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_cost_money"), PersistentDataType.INTEGER)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_cost_shards"), PersistentDataType.INTEGER)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_category"), PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "shop_back"), PersistentDataType.STRING);
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
