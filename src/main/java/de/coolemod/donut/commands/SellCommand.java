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

    public SellCommand(DonutPlugin plugin) { 
        this.plugin = plugin; 
    }
    
    private String toSmallCaps(String text) {
        return text.replace("A", "ᴀ").replace("B", "ʙ").replace("C", "ᴄ")
                .replace("D", "ᴅ").replace("E", "ᴇ").replace("F", "ғ")
                .replace("G", "ɢ").replace("H", "ʜ").replace("I", "ɪ")
                .replace("J", "ᴊ").replace("K", "ᴋ").replace("L", "ʟ")
                .replace("M", "ᴍ").replace("N", "ɴ").replace("O", "ᴏ")
                .replace("P", "ᴘ").replace("Q", "ǫ").replace("R", "ʀ")
                .replace("S", "s").replace("T", "ᴛ").replace("U", "ᴜ")
                .replace("V", "ᴠ").replace("W", "ᴡ").replace("X", "x")
                .replace("Y", "ʏ").replace("Z", "ᴢ");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { 
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!"); 
            return true; 
        }
        
        Player p = (Player) sender;
        
        // /sell ohne Argumente - öffnet GUI
        if (args.length == 0) {
            new de.coolemod.donut.gui.SellGUI(plugin).open(p);
            return true;
        }
        
        // /sell hand - nur Item in der Hand verkaufen
        if (args[0].equalsIgnoreCase("hand")) {
            return sellHand(p);
        }
        
        // /sell all - gesamtes Inventar verkaufen
        if (args[0].equalsIgnoreCase("all")) {
            return sellAll(p);
        }
        
        p.sendMessage("");
        p.sendMessage("§8┃ §6§lSELL §8┃ §7Verfügbare Befehle:");
        p.sendMessage("§8  ▸ §e/sell §8- §7Öffnet das GUI");
        p.sendMessage("§8  ▸ §e/sell hand §8- §7Item in der Hand");
        p.sendMessage("§8  ▸ §e/sell all §8- §7Alle Items verkaufen");
        p.sendMessage("");
        return true;
    }
    
    private boolean sellHand(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        
        if (hand == null || hand.getType().isAir()) {
            p.sendMessage("§8┃ §6§lSELL §8┃ §7Halte ein Item in der Hand!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        double worth = plugin.getWorthManager().getWorth(hand);
        
        if (worth <= 0) {
            String itemName = formatItemName(hand.getType().name());
            p.sendMessage("§8┃ §6§lSELL §8┃ §c" + itemName + " §7ist nicht verkaufbar!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        int amount = hand.getAmount();
        String itemName = formatItemName(hand.getType().name());
        double total = worth * amount;
        
        p.getInventory().setItemInMainHand(null);
        plugin.getEconomy().deposit(p.getUniqueId(), total);
        
        p.sendMessage("");
        p.sendMessage("§8┃ §6§lSELL §8┃ §a" + amount + "x §f" + itemName + " §7verkauft!");
        p.sendMessage("§8  ▸ §7Erhalten: §a+$" + String.format("%.2f", total));
        p.sendMessage("§8  ▸ §7Kontostand: §e$" + String.format("%.2f", plugin.getEconomy().getBalance(p.getUniqueId())));
        p.sendMessage("");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        return true;
    }
    
    private boolean sellAll(Player p) {
        double total = 0.0;
        int itemsSold = 0;
        int stacksSold = 0;
        org.bukkit.inventory.PlayerInventory inv = p.getInventory();
        
        // Verkaufe alle Items (außer Hotbar und Rüstung)
        for (int i = 9; i < 36; i++) {
            ItemStack is = inv.getItem(i);
            if (is == null || is.getType().isAir()) continue;
            
            double worth = plugin.getWorthManager().getWorth(is);
            if (worth <= 0) continue;
            
            total += worth * is.getAmount();
            itemsSold += is.getAmount();
            stacksSold++;
            inv.setItem(i, null);
        }
        
        if (total <= 0) {
            p.sendMessage("§8┃ §6§lSELL §8┃ §7Keine verkaufbaren Items gefunden!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return true;
        }
        
        plugin.getEconomy().deposit(p.getUniqueId(), total);
        
        p.sendMessage("");
        p.sendMessage("§8┃ §6§lSELL §8┃ §aInventar verkauft!");
        p.sendMessage("§8  ▸ §7Items: §f" + itemsSold + "x §8(§7" + stacksSold + " Stacks§8)");
        p.sendMessage("§8  ▸ §7Erhalten: §a+$" + String.format("%.2f", total));
        p.sendMessage("§8  ▸ §7Kontostand: §e$" + String.format("%.2f", plugin.getEconomy().getBalance(p.getUniqueId())));
        p.sendMessage("");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        return true;
    }
    
    private String formatItemName(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        
        for (String part : parts) {
            if (result.length() > 0) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0)));
            result.append(part.substring(1));
        }
        
        return result.toString();
    }
}
