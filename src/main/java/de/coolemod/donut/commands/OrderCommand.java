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
        if (!(sender instanceof Player)) { sender.sendMessage("Nur Spieler."); return true; }
        Player p = (Player) sender;
        if (args.length == 0) {
            new de.coolemod.donut.gui.OrdersGUI(plugin).open(p);
            return true;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("create")) {
            int amount; double price;
            try { amount = Integer.parseInt(args[1]); price = Double.parseDouble(args[2]); } catch (NumberFormatException e) { p.sendMessage("Ungültige Zahlen."); return true; }
            ItemStack inHand = p.getInventory().getItemInMainHand();
            if (inHand == null || inHand.getType().isAir()) { p.sendMessage("Halte ein Item, das die Order repräsentiert."); return true; }
            ItemStack itemType = inHand.clone(); itemType.setAmount(1);
            String id = plugin.getOrdersManager().createOrder(p.getUniqueId(), itemType, amount, price);
            if (id == null) { p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cNicht genug Geld, um die Order sofort zu bezahlen."); return true; }
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§aOrder erstellt (ID: " + id + ").");
            return true;
        }

        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "Benutze /order oder /order create <amount> <price> (halte Item)");
        return true;
    }
}