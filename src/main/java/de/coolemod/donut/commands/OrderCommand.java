package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * /order - Erstellen von Orders (einfacher Demo-Handler)
 */
public class OrderCommand implements CommandExecutor {
    private final DonutPlugin plugin;

    public OrderCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("§cDieser Befehl ist nur für Spieler!"); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            new de.coolemod.donut.gui.OrdersGUI(plugin).open(p);
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            int amount; double price;
            try { amount = Integer.parseInt(args[1]); price = Double.parseDouble(args[2]); } catch (NumberFormatException e) { 
                p.sendMessage("§8┃ §d§lORDER §8┃ §cUngültige Zahlen!"); 
                return true; 
            }
            ItemStack inHand = p.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType().isAir()) { 
                p.sendMessage("§8┃ §d§lORDER §8┃ §7Halte ein Item in der Hand!"); 
                return true; 
            }
            ItemStack itemType = inHand.clone(); itemType.setAmount(1);
            String id = plugin.getOrdersManager().createOrder(p.getUniqueId(), itemType, amount, price);
            if (id == null) { 
                p.sendMessage("§8┃ §d§lORDER §8┃ §cNicht genug Geld!"); 
                return true; 
            }
            p.sendMessage("§8┃ §d§lORDER §8┃ §aOrder erstellt! §8(§7ID: §f" + id + "§8)");
            return true;
        }

        p.sendMessage("§8┃ §d§lORDER §8┃ §7Verwendung: §e/order §7oder §e/order create <anzahl> <preis>");
        return true;
    }
}