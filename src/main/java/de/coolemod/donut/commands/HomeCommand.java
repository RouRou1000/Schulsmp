package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.systems.HomeGUI;
import de.coolemod.donut.systems.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HomeCommand implements CommandExecutor, TabCompleter {
    private final DonutPlugin plugin;
    private final HomeManager homeManager;
    private HomeGUI homeGUI;

    public HomeCommand(DonutPlugin plugin, HomeManager homeManager) {
        this.plugin = plugin;
        this.homeManager = homeManager;
    }

    public void setHomeGUI(HomeGUI homeGUI) {
        this.homeGUI = homeGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl ist nur für Spieler!");
            return true;
        }

        // /homes opens GUI
        if (command.getName().equalsIgnoreCase("homes")) {
            if (homeGUI != null) {
                player.openInventory(homeGUI.createHomesGUI(player));
            } else {
                player.sendMessage("§8┃ §6§lHOME §8┃ §cSystem nicht verfügbar!");
            }
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "set", "sethome" -> {
                if (args.length < 2) {
                    player.sendMessage("§8┃ §6§lHOME §8┃ §7Verwendung: §e/home set <name>");
                    return true;
                }
                // Check limit
                if (homeManager.getHomeCount(player) >= HomeGUI.MAX_HOMES) {
                    player.sendMessage("§8┃ §6§lHOME §8┃ §cMaximum von §e" + HomeGUI.MAX_HOMES + " §cHomes erreicht!");
                    return true;
                }
                homeManager.setHome(player, args[1]);
            }
            case "del", "delete", "remove" -> {
                if (args.length < 2) {
                    player.sendMessage("§8┃ §6§lHOME §8┃ §7Verwendung: §e/home del <name>");
                    return true;
                }
                homeManager.deleteHome(player, args[1]);
            }
            case "list", "gui" -> {
                if (homeGUI != null) {
                    player.openInventory(homeGUI.createHomesGUI(player));
                } else {
                    List<String> homes = homeManager.getHomeNames(player);
                    if (homes.isEmpty()) {
                        player.sendMessage("§8┃ §6§lHOME §8┃ §7Du hast keine Homes.");
                    } else {
                        player.sendMessage("");
                        player.sendMessage("§8┃ §6§lHOME §8┃ §7Deine Homes: §e" + homes.size() + "§7/§f" + HomeGUI.MAX_HOMES);
                        for (String home : homes) {
                            player.sendMessage("§8  ▸ §f" + home);
                        }
                        player.sendMessage("");
                    }
                }
            }
            default -> {
                // Try to teleport to home
                homeManager.teleportHome(player, subCommand);
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("");
        player.sendMessage("§8┃ §6§lHOME §8┃ §7Verfügbare Befehle:");
        player.sendMessage("§8  ▸ §e/home <name> §8- §7Teleport");
        player.sendMessage("§8  ▸ §e/home set <name> §8- §7Home setzen");
        player.sendMessage("§8  ▸ §e/home del <name> §8- §7Home löschen");
        player.sendMessage("§8  ▸ §e/homes §8- §7Öffne GUI");
        player.sendMessage("§8  ┗ §7Max: §e" + HomeGUI.MAX_HOMES + " Homes");
        player.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return null;

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("set", "del", "list"));
            completions.addAll(homeManager.getHomeNames(player));
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("remove"))) {
            return homeManager.getHomeNames(player).stream()
                .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                .collect(Collectors.toList());
        }

        return completions;
    }
}
