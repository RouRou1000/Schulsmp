package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;

/**
 * /sell [hand|all] - verkauft Items aus dem Inventar basierend auf WorthManager
 */
public class SellCommand implements CommandExecutor {
    private final DonutPlugin plugin;

    public SellCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("§c✗ Nur Spieler können das nutzen."); return true; }
        Player p = (Player) sender;
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        
        // /sell ohne Argumente - öffnet GUI
        if (args.length == 0) {
            new de.coolemod.donut.gui.SellGUI(plugin).open(p);
            return true;
        }
        
        // /sell hand - nur Item in der Hand verkaufen
        if (args.length > 0 && args[0].equalsIgnoreCase("hand")) {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                p.sendMessage(prefix + "§x§F§F§5§0§0§0 ✗ §cᴅᴜ ʜäʟᴛѕᴛ ᴋᴇɪɴ ɪᴛᴇᴍ ɪɴ ᴅᴇʀ ʜᴀɴᴅ.");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            double worth = plugin.getWorthManager().getWorth(hand);
            if (worth <= 0) {
                p.sendMessage(prefix + "§x§F§F§5§0§0§0 ✗ §cᴅɪᴇѕᴇѕ ɪᴛᴇᴍ ᴋᴀɴɴ ɴɪᴄʜᴛ ᴠᴇʀᴋᴀᴜꜰᴛ ᴡᴇʀᴅᴇɴ.");
                p.sendMessage(prefix + "§8▸ §7ᴛɪᴘᴘ: ɪᴛᴇᴍ ᴍᴜѕѕ ᴇɪɴᴇɴ ᴡᴇʀᴛ ʜᴀʙᴇɴ.");
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return true;
            }
            int amount = hand.getAmount();
            String itemName = formatMaterial(hand.getType().name());
            double total = worth * amount;
            p.getInventory().setItemInMainHand(null);
            plugin.getEconomy().deposit(p.getUniqueId(), total);
            p.sendMessage(prefix + "§x§0§0§F§F§0§0 ✔ §a" + amount + "x §f" + itemName + " §aꜰüʀ §x§F§F§F§0§0§0§l$" + String.format("%.2f", total) + " §aᴠᴇʀᴋᴀᴜꜰᴛ!");
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return true;
        }
        
        // /sell oder /sell all - gesamtes Inventar verkaufen
        double total = 0.0;
        int itemsSold = 0;
        org.bukkit.inventory.PlayerInventory inv = p.getInventory();
        
        // Armor und Hotbar-Item 0-8 überspringen (optional)
        for (int i = 9; i < 36; i++) {
            ItemStack is = inv.getItem(i);
            if (is == null || is.getType().isAir()) continue;
            double worth = plugin.getWorthManager().getWorth(is);
            if (worth <= 0) continue;
            total += worth * is.getAmount();
            itemsSold += is.getAmount();
            inv.setItem(i, null);
        }
        
        if (total <= 0) {
            p.sendMessage(prefix + "§x§F§F§5§0§0§0 ✗ §cᴅᴜ ʜᴀѕᴛ ɴɪᴄʜᴛѕ ᴢᴜ ᴠᴇʀᴋᴀᴜꜰᴇɴ.");
            p.sendMessage(prefix + "§8▸ §7ᴛɪᴘᴘ: ɪᴛᴇᴍѕ ᴍüѕѕᴇɴ ᴇɪɴᴇɴ ᴡᴇʀᴛ ʜᴀʙᴇɴ.");
            p.sendMessage(prefix + "§8▸ §7ɴᴜᴛᴢᴇ §f/ᴡᴏʀᴛʜ §7ᴜᴍ ɪᴛᴇᴍ-ᴡᴇʀᴛᴇ ᴢᴜ ᴘʀüꜰᴇɴ.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        plugin.getEconomy().deposit(p.getUniqueId(), total);
        p.sendMessage(prefix + "§x§0§0§F§F§0§0 ✔ §x§F§F§F§0§0§0§l" + itemsSold + " ɪᴛᴇᴍѕ §aꜰüʀ §x§F§F§F§0§0§0§l$" + String.format("%.2f", total) + " §aᴠᴇʀᴋᴀᴜꜰᴛ!");
        p.sendMessage(prefix + "§8▸ §7ᴋᴏɴᴛᴏѕᴛᴀɴᴅ: §x§0§0§F§F§0§0§l$" + String.format("%.2f", plugin.getEconomy().getBalance(p.getUniqueId())));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        return true;
    }
    
    private String formatMaterial(String name) {
        return name.toLowerCase().replace("_", " ");
    }
}
