package de.coolemod.donut.commands;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.CrateManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * /crateadmin <subcommands>
 * - list
 * - create <id> <display> <keyName>
 * - delete <id>
 * - addpool <id> <MATERIAL:amount:weight>
 */
public class CrateAdminCommand implements CommandExecutor {
    private final DonutPlugin plugin;
    public CrateAdminCommand(DonutPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("donut.admin")) { sender.sendMessage(plugin.getConfig().getString("messages.prefix", "") + "§cKeine Berechtigung."); return true; }
        if (args.length == 0) { sender.sendMessage("Usage: /crateadmin <list|create|delete|addpool|addguaranteed|addbundle|settier|test|info>"); return true; }
        switch (args[0].toLowerCase()) {
            case "list":
                sender.sendMessage("Kisten:");
                for (String id : plugin.getCrateManager().getCrateIds()) sender.sendMessage(" - " + id);
                return true;
            case "create":
                if (args.length < 4) { sender.sendMessage("Usage: /crateadmin create <id> <display> <keyName>"); return true; }
                String id = args[1]; String display = args[2]; String key = args[3];
                if (plugin.getCrateManager().hasCrate(id)) { sender.sendMessage("Kiste bereits vorhanden."); return true; }
                plugin.getConfig().set("crates." + id + ".display", display);
                plugin.getConfig().set("crates." + id + ".key", key);
                plugin.getConfig().set("crates." + id + ".animation.ticks", 2);
                plugin.getConfig().set("crates." + id + ".animation.cycles", 30);
                plugin.saveConfig();
                plugin.getCrateManager().reload();
                sender.sendMessage("Kiste erstellt: " + id);
                return true;
            case "delete":
                if (args.length < 2) { sender.sendMessage("Usage: /crateadmin delete <id>"); return true; }
                String did = args[1];
                if (!plugin.getCrateManager().hasCrate(did)) { sender.sendMessage("Kiste nicht gefunden."); return true; }
                plugin.getConfig().set("crates." + did, null);
                plugin.saveConfig();
                plugin.getCrateManager().reload();
                sender.sendMessage("Kiste gelöscht: " + did);
                return true;
            case "addpool":
                if (args.length < 3) { sender.sendMessage("Usage: /crateadmin addpool <id> <MATERIAL:amount:weight>"); return true; }
                String cid = args[1];
                String entry = args[2];
                if (!plugin.getCrateManager().hasCrate(cid)) { sender.sendMessage("Kiste nicht gefunden."); return true; }
                // Anhängen an config
                java.util.List<String> pool = plugin.getConfig().getStringList("crates." + cid + ".pool");
                pool.add(entry);
                plugin.getConfig().set("crates." + cid + ".pool", pool);
                plugin.saveConfig();
                plugin.getCrateManager().reload();
                sender.sendMessage("Pool-Eintrag hinzugefügt.");
                return true;
            case "addguaranteed":
                if (args.length < 3) { sender.sendMessage("Usage: /crateadmin addguaranteed <id> <MATERIAL:amount[:namedKey]>"); return true; }
                String gid = args[1];
                String gentry = args[2];
                if (!plugin.getCrateManager().hasCrate(gid)) { sender.sendMessage("Kiste nicht gefunden."); return true; }
                java.util.List<String> gl = plugin.getConfig().getStringList("crates." + gid + ".guaranteed");
                gl.add(gentry);
                plugin.getConfig().set("crates." + gid + ".guaranteed", gl);
                plugin.saveConfig();
                plugin.getCrateManager().reload();
                sender.sendMessage("Guaranteed-Eintrag hinzugefügt.");
                return true;
            case "addbundle":
                if (args.length < 3) { sender.sendMessage("Usage: /crateadmin addbundle <id> <bundleString e.g. GOLD_INGOT:10;EMERALD:2>"); return true; }
                String bid = args[1];
                String bentry = args[2];
                if (!plugin.getCrateManager().hasCrate(bid)) { sender.sendMessage("Kiste nicht gefunden."); return true; }
                java.util.List<String> bl = plugin.getConfig().getStringList("crates." + bid + ".bundles");
                bl.add(bentry);
                plugin.getConfig().set("crates." + bid + ".bundles", bl);
                plugin.saveConfig();
                plugin.getCrateManager().reload();
                sender.sendMessage("Bundle hinzugefügt.");
                return true;
            case "settier":
                if (args.length < 3) { sender.sendMessage("Usage: /crateadmin settier <id> <tier>"); return true; }
                String tid = args[1];
                String tier = args[2];
                if (!plugin.getCrateManager().hasCrate(tid)) { sender.sendMessage("Kiste nicht gefunden."); return true; }
                plugin.getConfig().set("crates." + tid + ".tier", tier);
                plugin.saveConfig();
                plugin.getCrateManager().reload();
                sender.sendMessage("Tier gesetzt.");
                return true;
            case "test":
                if (args.length < 2) { sender.sendMessage("Usage: /crateadmin test <crateId> [player]"); return true; }
                String testId = args[1];
                String targetName = args.length >= 3 ? args[2] : null;
                org.bukkit.entity.Player target;
                if (targetName == null) {
                    if (!(sender instanceof org.bukkit.entity.Player)) { sender.sendMessage("Nur Spieler können sich selbst testen."); return true; }
                    target = (org.bukkit.entity.Player) sender;
                } else {
                    target = org.bukkit.Bukkit.getPlayer(targetName);
                    if (target == null) { sender.sendMessage("Spieler nicht online."); return true; }
                }
                if (!plugin.getCrateManager().hasCrate(testId)) { sender.sendMessage("Kiste nicht gefunden."); return true; }
                // Open without consuming key (admin test)
                plugin.getCrateManager().openCrateAnimated(target, testId, true);
                sender.sendMessage("Test ausgeführt.");
                return true;
            case "info":
                if (args.length < 2) { sender.sendMessage("Usage: /crateadmin info <id>"); return true; }
                String iid = args[1];
                if (!plugin.getCrateManager().hasCrate(iid)) { sender.sendMessage("Kiste nicht gefunden."); return true; }
                CrateManager.Crate info = plugin.getCrateManager().getCrate(iid);
                sender.sendMessage("Kiste: " + iid + " (" + info.display + ")");
                sender.sendMessage("Tier: " + (info.tier == null ? "none" : info.tier));
                sender.sendMessage("Pool-Größe: " + info.pool.size() + " | Guaranteed: " + info.guaranteed.size() + " | Bundles: " + info.bundles.size());
                return true;
            default:
                sender.sendMessage("Unbekannter Subcommand.");
                return true;
        }
    }
}