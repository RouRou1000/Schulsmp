package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.utils.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

public class BaltopCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    private static final int PLAYERS_PER_PAGE = 28;

    public BaltopCommand(DonutPlugin plugin) {
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cNur Spieler können diesen Befehl nutzen.");
            return true;
        }

        int page = 0;
        if (args.length > 0) {
            try {
                page = Math.max(0, Integer.parseInt(args[0]) - 1);
            } catch (NumberFormatException ignored) {}
        }

        player.openInventory(createBaltopGUI(player.getUniqueId(), page));
        return true;
    }

    public Inventory createBaltopGUI(UUID viewer, int page) {
        // Get all balances sorted descending
        List<Map.Entry<UUID, Double>> sorted = plugin.getEconomy().getBalances().entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        int totalPages = Math.max(1, (sorted.size() + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, "§6§l" + toSmallCaps("BALTOP") + " §8(" + (page + 1) + "/" + totalPages + ")");

        // Fill borders
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName("§8⬛");
        border.setItemMeta(borderMeta);
        for (int i : new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53}) {
            inv.setItem(i, border);
        }

        // Info header
        ItemStack info = new ItemStack(Material.GOLD_INGOT);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName("§6§l" + toSmallCaps("REICHSTE SPIELER"));
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§8────────────────");
        infoLore.add("§7Spieler gesamt: §f" + sorted.size());
        infoLore.add("§7Seite: §f" + (page + 1) + "§7/§f" + totalPages);
        infoLore.add("§8────────────────");

        // Show viewer's rank
        int viewerRank = -1;
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getKey().equals(viewer)) {
                viewerRank = i + 1;
                break;
            }
        }
        if (viewerRank > 0) {
            infoLore.add("§7Dein Rang: §e#" + viewerRank);
            infoLore.add("§7Dein Geld: §a" + NumberFormatter.formatMoney(plugin.getEconomy().getBalance(viewer)));
        }
        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        inv.setItem(4, info);

        // Show players
        int start = page * PLAYERS_PER_PAGE;
        int end = Math.min(start + PLAYERS_PER_PAGE, sorted.size());

        int slot = 10;
        for (int i = start; i < end; i++) {
            if (slot == 17) slot = 19;
            if (slot == 26) slot = 28;
            if (slot == 35) slot = 37;
            if (slot >= 44) break;

            Map.Entry<UUID, Double> entry = sorted.get(i);
            int rank = i + 1;
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getKey());
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unbekannt";

            // Top 3 get special heads
            Material headMat = rank <= 3 ? Material.PLAYER_HEAD : Material.PLAYER_HEAD;
            ItemStack head = new ItemStack(headMat);
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            skullMeta.setOwningPlayer(offlinePlayer);

            String rankColor = switch (rank) {
                case 1 -> "§6§l";
                case 2 -> "§7§l";
                case 3 -> "§c§l";
                default -> "§e";
            };
            String rankPrefix = switch (rank) {
                case 1 -> "§6§l\uD83E\uDD47 ";
                case 2 -> "§7§l\uD83E\uDD48 ";
                case 3 -> "§c§l\uD83E\uDD49 ";
                default -> "§e#" + rank + " ";
            };

            skullMeta.setDisplayName(rankPrefix + "§f" + name);
            List<String> lore = new ArrayList<>();
            lore.add("§8────────────────");
            lore.add("§7Position: " + rankColor + "#" + rank);
            lore.add("§7Geld: §a" + NumberFormatter.formatMoney(entry.getValue()));
            lore.add("§8────────────────");

            if (entry.getKey().equals(viewer)) {
                lore.add("§e⭐ Das bist du!");
            }

            skullMeta.setLore(lore);
            head.setItemMeta(skullMeta);
            inv.setItem(slot, head);
            slot++;
        }

        if (sorted.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = empty.getItemMeta();
            emptyMeta.setDisplayName("§7Keine Spieler gefunden");
            empty.setItemMeta(emptyMeta);
            inv.setItem(22, empty);
        }

        // Page info
        ItemStack pageInfo = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        pageMeta.setDisplayName("§e§l📄 " + toSmallCaps("SEITE") + " §f" + (page + 1) + "§7/§f" + totalPages);
        pageInfo.setItemMeta(pageMeta);
        inv.setItem(49, pageInfo);

        // Prev page
        if (page > 0) {
            ItemStack prevBtn = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevBtn.getItemMeta();
            prevMeta.setDisplayName("§e§l◄ " + toSmallCaps("VORHERIGE SEITE"));
            prevBtn.setItemMeta(prevMeta);
            inv.setItem(45, prevBtn);
        }

        // Next page
        if (page < totalPages - 1) {
            ItemStack nextBtn = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextBtn.getItemMeta();
            nextMeta.setDisplayName("§e§l" + toSmallCaps("NÄCHSTE SEITE") + " ►");
            nextBtn.setItemMeta(nextMeta);
            inv.setItem(53, nextBtn);
        }

        return inv;
    }
}
