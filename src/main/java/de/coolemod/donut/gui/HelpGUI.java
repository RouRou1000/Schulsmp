package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

/**
 * Help-GUI: Zeigt alle verfügbaren Commands übersichtlich an
 */
public class HelpGUI {
    private final DonutPlugin plugin;

    public HelpGUI(DonutPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        Inventory inv = GUIUtils.createMenu("§8● §6§lHilfe §8●", 6);
        GUIUtils.fillBorders(inv, plugin);

        // === Kategorie: Geld & Handel ===
        inv.setItem(10, createItem(Material.EMERALD, "§a§l$ Geld & Handel",
            "§8────────────────",
            "§e/balance §8- §7Zeigt deinen Kontostand",
            "§e/pay <spieler> <betrag> §8- §7Überweise Geld",
            "§e/sell [hand|all] §8- §7Verkaufe Items",
            "§e/worth §8- §7Zeigt den Wert eines Items",
            "§8────────────────"
        ));

        // === Kategorie: Shop ===
        inv.setItem(11, createItem(Material.CHEST, "§6§l✦ Shops",
            "§8────────────────",
            "§e/shop §8- §7Öffnet den Item-Shop",
            "§e/shards buy §8- §7Öffnet den Shard-Shop",
            "§e/ah §8- §7Öffnet das Auktionshaus",
            "§e/order §8- §7Erstelle & verwalte Orders",
            "§e/crate §8- §7Öffnet das Kisten-Menü",
            "§8────────────────"
        ));

        // === Kategorie: Teleport ===
        inv.setItem(12, createItem(Material.ENDER_PEARL, "§b§l✦ Teleport",
            "§8────────────────",
            "§e/home <name> §8- §7Teleport zum Home",
            "§e/home set <name> §8- §7Setze ein Home",
            "§e/home del <name> §8- §7Lösche ein Home",
            "§e/homes §8- §7Öffnet die Home-GUI",
            "§e/tpa <spieler> §8- §7TPA-Anfrage senden",
            "§e/tpahere <spieler> §8- §7Spieler zu dir einladen",
            "§e/tpaccept §8- §7TPA annehmen",
            "§e/tpdeny §8- §7TPA ablehnen",
            "§e/spawn §8- §7Teleport zum Spawn",
            "§e/rtp §8- §7Random Teleport",
            "§8────────────────"
        ));

        // === Kategorie: Sonstiges ===
        inv.setItem(13, createItem(Material.BOOK, "§e§l✦ Sonstiges",
            "§8────────────────",
            "§e/help §8- §7Zeigt diese Hilfe",
            "§e/discord §8- §7Zeigt den Discord-Link",
            "§e/settings §8- §7Oeffnet deine Einstellungen",
            "§e/tutorial §8- §7Öffnet das Tutorial-Buch",
            "§e/msg <spieler> <text> §8- §7Private Nachricht senden",
            "§e/r <text> §8- §7Antwort auf letzte Nachricht",
            "§8────────────────"
        ));

        // Info-Item
        inv.setItem(4, createItem(Material.PLAYER_HEAD, "§6§l" + p.getName(),
            "§8────────────────",
            "§7Willkommen auf dem Server!",
            "§7Hover über die Items für",
            "§7eine Übersicht aller Commands.",
            "§8────────────────"
        ));

        GUIUtils.open(p, inv);
    }

    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack is = new ItemStack(mat);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        is.setItemMeta(m);
        return is;
    }
}
