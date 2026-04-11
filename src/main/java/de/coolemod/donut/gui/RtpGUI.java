package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class RtpGUI implements InventoryHolder {
    private final Inventory inventory;

    public RtpGUI() {
        this.inventory = Bukkit.createInventory(this, 27, "§a§lRTP §8- §7Welt wählen");

        // Glasscheiben als Rahmen
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName("§r");
        filler.setItemMeta(fillerMeta);
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        // Overworld - Slot 11
        ItemStack overworld = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta owMeta = overworld.getItemMeta();
        owMeta.setDisplayName("§a§lOverworld");
        owMeta.setLore(List.of("", "§7Teleportiert dich an eine", "§7zufällige Position in der §aOverworld§7.", "", "§e▸ Klicke zum Teleportieren"));
        overworld.setItemMeta(owMeta);
        inventory.setItem(11, overworld);

        // Nether - Slot 13
        ItemStack nether = new ItemStack(Material.NETHERRACK);
        ItemMeta netherMeta = nether.getItemMeta();
        netherMeta.setDisplayName("§c§lNether");
        netherMeta.setLore(List.of("", "§7Teleportiert dich an eine", "§7zufällige Position im §cNether§7.", "", "§e▸ Klicke zum Teleportieren"));
        nether.setItemMeta(netherMeta);
        inventory.setItem(13, nether);

        // End - Slot 15 (locked)
        ItemStack end = new ItemStack(Material.BARRIER);
        ItemMeta endMeta = end.getItemMeta();
        endMeta.setDisplayName("§8§lThe End");
        endMeta.setLore(List.of("", "§c✖ Noch nicht freigeschalten!", "", "§7Das End wird bald verfügbar sein."));
        end.setItemMeta(endMeta);
        inventory.setItem(15, end);
    }

    public void open(Player p) {
        p.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
