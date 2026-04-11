package de.coolemod.donut.utils;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

public final class TutorialBook {
    private TutorialBook() {}

    public static void open(DonutPlugin plugin, Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.setTitle("Tutorial");
        meta.setAuthor("Server");
        meta.setPages(List.of(
            page(
                "§6§lSCHUL SMP",
                "§8━━━━━━━━━━",
                "",
                "§0Willkommen,",
                "§6" + player.getName() + "§0!",
                "",
                "§0Dieses Tutorial zeigt",
                "§0dir die wichtigsten",
                "§0Systeme für deinen",
                "§0Start.",
                "",
                "§8Nutze §6/tutorial§8,",
                "§8um es erneut zu öffnen."
            ),
            page(
                "§6§lSCHNELLSTART",
                "§8━━━━━━━━━━",
                "",
                "§0§l/spawn§r",
                "§0Teleport zum Spawn.",
                "",
                "§0§l/rtp§r",
                "§0Öffnet die Teleport-",
                "§0GUI für Overworld",
                "§0oder Nether.",
                "",
                "§8Nicht bewegen, sonst",
                "§8bricht der Countdown",
                "§8ab."
            ),
            page(
                "§6§lHOMES",
                "§8━━━━━━━━━━",
                "",
                "§0§l/homes§r",
                "§0Home-Übersicht (GUI).",
                "",
                "§0§l/home set <name>§r",
                "§0Setzt einen Home.",
                "",
                "§0§l/home <name>§r",
                "§0Teleportiert dich.",
                "",
                "§0Max. §65 Homes§0 pro",
                "§0Spieler."
            ),
            page(
                "§6§lGELD",
                "§8━━━━━━━━━━",
                "",
                "§0§l/balance§r",
                "§0Zeigt dein Guthaben.",
                "",
                "§0§l/sell hand§r §0/ §l/sell all§r",
                "§0Verkauft deine Items.",
                "",
                "§0§l/worth§r",
                "§0Zeigt den Wert.",
                "",
                "§0§l/pay <name> <$>§r",
                "§0Sendet Geld."
            ),
            page(
                "§6§lSELL MULTI",
                "§8━━━━━━━━━━",
                "",
                "§0§l/sellmulti§r",
                "§0Zeigt deine Sell-",
                "§0Multiplikatoren.",
                "",
                "§0Je mehr du in einer",
                "§0Kategorie verkaufst,",
                "§0desto höher wird dein",
                "§0Multiplikator.",
                "",
                "§8Von §e1.1x §8bei $1M",
                "§8bis §64.0x §8bei $10B."
            ),
            page(
                "§6§lAUKTIONEN",
                "§8━━━━━━━━━━",
                "",
                "§0§l/ah§r",
                "§0Öffnet das Auktions-",
                "§0haus.",
                "",
                "§0Kaufe und verkaufe",
                "§0Items an andere",
                "§0Spieler.",
                "",
                "§0Preiseingabe per",
                "§0Sign-GUI."
            ),
            page(
                "§6§lORDERS",
                "§8━━━━━━━━━━",
                "",
                "§0§l/order§r",
                "§0Öffnet das Order-",
                "§0System.",
                "",
                "§0Erstelle Kaufanfragen",
                "§0oder beliefere andere.",
                "",
                "§0Menge & Preis werden",
                "§0per Sign eingegeben."
            ),
            page(
                "§6§lSPAWNER",
                "§8━━━━━━━━━━",
                "",
                "§0Spawner kaufst du im",
                "§0Shop. Drops sammelst",
                "§0du per Rechtsklick.",
                "",
                "§0§lSilk Touch§r§0 baut",
                "§0Spawner ab.",
                "",
                "§0Mit §6Sneak§0 baust du",
                "§064 Spawner§0 auf einmal",
                "§0ab/auf."
            ),
            page(
                "§6§lKISTEN & CHAT",
                "§8━━━━━━━━━━",
                "",
                "§0§l/crate§r",
                "§0Kisten & Rewards.",
                "",
                "§0§l/msg <name> <text>§r",
                "§0Private Nachricht.",
                "",
                "§0§l/r <text>§r",
                "§0Schnelle Antwort.",
                "",
                "§0§l/help§r",
                "§0Alle Commands."
            ),
            page(
                "§6§lDEIN START",
                "§8━━━━━━━━━━",
                "",
                "§01. §6/rtp §0→ raus!",
                "",
                "§02. §6/home set §0→",
                "§0   erstes Home.",
                "",
                "§03. §6/sell §0→ Loot",
                "§0   verkaufen.",
                "",
                "§04. §6/ah §0& §6/order§0 →",
                "§0   handeln.",
                "",
                "§0Viel Spaß! §6♥"
            )
        ));

        book.setItemMeta(meta);

        int heldSlot = player.getInventory().getHeldItemSlot();
        ItemStack previousItem = player.getInventory().getItem(heldSlot);
        player.getInventory().setItem(heldSlot, book);
        player.openBook(book);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                player.getInventory().setItem(heldSlot, previousItem);
                player.updateInventory();
            }
        }, 1L);
    }

    private static String page(String... lines) {
        return String.join("\n", lines);
    }
}