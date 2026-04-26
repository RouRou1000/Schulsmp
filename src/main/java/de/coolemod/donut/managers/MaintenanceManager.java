package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;

public class MaintenanceManager {
    private static final String CONFIG_KEY = "maintenance.enabled";
    private static final Set<String> BYPASS_GROUPS = Set.of("owner", "admin", "mod", "builder");

    private final DonutPlugin plugin;
    private boolean enabled;
    private BukkitTask countdownTask;

    public MaintenanceManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean(CONFIG_KEY, false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void startMaintenance(CommandSender sender) {
        if (enabled) {
            sender.sendMessage("§8┃ §6§lMAINTENCE §8┃ §7Maintenance ist bereits aktiv.");
            return;
        }

        enabled = true;
        persist();

        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§8┃ §6§lUPDATE §8┃ §eDer Server geht in §6 30 Sekunden §ein Maintenance.");
        Bukkit.broadcastMessage("§8┃ §7Bitte suche einen sicheren Platz. Nur §fOwner/Admin/Mod/Builder §7bleiben online.");
        Bukkit.broadcastMessage("");

        final int[] secondsLeft = {30};
        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int seconds = secondsLeft[0];

            if (seconds == 30 || seconds == 20 || seconds == 10 || seconds <= 5) {
                Bukkit.broadcastMessage("§8┃ §6§lUPDATE §8┃ §eMaintenance in §6" + seconds + "s§e. Nur Team-Ränge bleiben online.");
                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendTitle("§6§lUPDATE", "§eMaintenance in §6" + seconds + "s", 0, 25, 5);
                }
            }

            if (seconds <= 0) {
                kickNonBypassPlayers();
                cancelCountdown();
                Bukkit.broadcastMessage("§8┃ §6§lMAINTENCE §8┃ §aMaintenance ist jetzt aktiv.");
                return;
            }

            secondsLeft[0] = seconds - 1;
        }, 0L, 20L);

        sender.sendMessage("§8┃ §6§lMAINTENCE §8┃ §aCountdown gestartet. Maintenance bleibt auch nach Restart aktiv.");
    }

    public void stopMaintenance(CommandSender sender) {
        if (!enabled) {
            sender.sendMessage("§8┃ §6§lMAINTENCE §8┃ §7Maintenance ist bereits deaktiviert.");
            return;
        }

        enabled = false;
        persist();
        cancelCountdown();
        Bukkit.broadcastMessage("§8┃ §6§lMAINTENCE §8┃ §aMaintenance wurde deaktiviert. Der Server ist wieder offen.");
        sender.sendMessage("§8┃ §6§lMAINTENCE §8┃ §aMaintenance deaktiviert.");
    }

    public boolean canBypass(Player player) {
        if (player == null) {
            return false;
        }
        if (player.isOp() || player.hasPermission("donut.maintenance.bypass") || player.hasPermission("donut.admin")) {
            return true;
        }

        try {
            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user == null) {
                return false;
            }
            if (BYPASS_GROUPS.contains(user.getPrimaryGroup().toLowerCase())) {
                return true;
            }
            for (InheritanceNode node : user.getNodes(NodeType.INHERITANCE)) {
                if (BYPASS_GROUPS.contains(node.getGroupName().toLowerCase())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    public String getKickMessage() {
        return "§cDer Server ist derzeit im Maintenance-Modus.\n§7Es wird gerade ein Update eingespielt.\n§eBitte versuche es später erneut.";
    }

    private void kickNonBypassPlayers() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!canBypass(online)) {
                online.kickPlayer(getKickMessage());
            } else {
                online.sendMessage("§8┃ §6§lMAINTENCE §8┃ §7Du darfst während der Maintenance online bleiben.");
            }
        }
    }

    private void cancelCountdown() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
    }

    private void persist() {
        plugin.getConfig().set(CONFIG_KEY, enabled);
        plugin.saveConfig();
    }
}