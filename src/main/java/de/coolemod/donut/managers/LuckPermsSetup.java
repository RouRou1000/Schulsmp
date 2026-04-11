package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.luckperms.api.node.types.WeightNode;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Erstellt automatisch alle LuckPerms-Gruppen mit Prefixes, Suffixes,
 * Permissions und Vererbung beim Server-Start.
 * Neue Spieler bekommen automatisch den "spieler"-Rang.
 */
public class LuckPermsSetup {
    private static final String AUTO_OWNER_PLAYER = "rourou1000";

    private final DonutPlugin plugin;
    private final Logger log;
    private LuckPerms luckPerms;

    // Gruppen-Definition: Name → Weight
    private static final Map<String, Integer> GROUPS = Map.of(
        "owner", 100,
        "builder", 90,
        "admin", 80,
        "mod", 70,
        "spieler", 0
    );

    // Prefixes
    private static final Map<String, String> PREFIXES = Map.of(
        "owner", "§4§lOwner",
        "admin", "§c§lAdmin",
        "mod", "§c§lMod",
        "builder", "§a§lBuilder",
        "spieler", "§7Spieler"
    );

    // Suffixes (Namensfarbe)
    private static final Map<String, String> SUFFIXES = Map.of(
        "owner", "§4",
        "admin", "§c",
        "mod", "§c",
        "builder", "§a",
        "spieler", "§7"
    );

    // Vererbung: Kind → Eltern
    private static final Map<String, String> PARENTS = Map.of(
        "owner", "builder",
        "builder", "admin",
        "admin", "mod",
        "mod", "spieler"
    );

    // Permissions pro Gruppe
    private static final Map<String, List<String>> PERMISSIONS = Map.of(
        "owner", List.of("*"),
        "admin", List.of(
            "donut.admin",
            // Minecraft
            "minecraft.command.gamemode",
            "minecraft.command.teleport",
            "minecraft.command.kick",
            "minecraft.command.ban",
            "minecraft.command.op",
            "minecraft.command.give",
            "minecraft.command.time",
            "minecraft.command.weather",
            "bukkit.command.gamemode",
            // WorldEdit
            "worldedit.*",
            // WorldGuard
            "worldguard.*",
            // Multiverse
            "multiverse.core.*",
            "multiverse.access.*",
            // Chunky
            "chunky.command.chunky",
            // GrimAC
            "grim.alerts",
            "grim.alerts.enable-on-join",
            "grim.brand",
            "grim.nosetback",
            "grim.exempt"
        ),
        "mod", List.of(
            "donut.ac",
            "donut.money",
            "donut.shards",
            "donut.wipe",
            "donut.crate.admin",
            "donut.balance.others",
            // Minecraft
            "minecraft.command.kick",
            "minecraft.command.ban",
            "minecraft.command.teleport",
            "minecraft.command.gamemode",
            // GrimAC
            "grim.alerts",
            "grim.alerts.enable-on-join",
            "grim.brand"
        ),
        "builder", List.of(
            // Minecraft
            "minecraft.command.gamemode",
            "minecraft.command.teleport",
            "bukkit.command.gamemode",
            // WorldEdit
            "worldedit.*",
            // WorldGuard
            "worldguard.region.define.*",
            "worldguard.region.claim",
            "worldguard.region.info.*",
            "worldguard.region.list"
        ),
        "spieler", List.of()
    );

    public LuckPermsSetup(DonutPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    /**
     * Startet das automatische Setup. Wartet 2 Sekunden damit LuckPerms vollständig geladen ist.
     */
    public void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("LuckPerms") == null) {
            log.warning("LuckPerms nicht gefunden! Gruppen-Setup übersprungen.");
            return;
        }

        // Etwas verzögern damit LuckPerms vollständig initialisiert ist
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                luckPerms = LuckPermsProvider.get();
                log.info("[LuckPerms] Starte automatisches Gruppen-Setup...");
                setupGroups();
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> ensureAutoOwner(), 20L);
            } catch (Exception e) {
                log.warning("[LuckPerms] Konnte LuckPerms-API nicht laden: " + e.getMessage());
            }
        }, 40L); // 2 Sekunden warten
    }

    private void setupGroups() {
        GroupManager gm = luckPerms.getGroupManager();

        for (var entry : GROUPS.entrySet()) {
            String name = entry.getKey();
            int weight = entry.getValue();

            // Gruppe erstellen falls nicht vorhanden
            CompletableFuture<Group> future = gm.createAndLoadGroup(name);
            future.thenAcceptAsync(group -> {
                boolean changed = false;

                // Weight setzen falls nicht vorhanden
                if (group.getWeight().isEmpty() || group.getWeight().getAsInt() != weight) {
                    group.data().clear(n -> n instanceof WeightNode);
                    group.data().add(WeightNode.builder(weight).build());
                    changed = true;
                }

                // Prefix setzen falls nicht vorhanden
                String prefix = PREFIXES.get(name);
                if (prefix != null) {
                    String currentPrefix = group.getCachedData().getMetaData().getPrefix();
                    if (currentPrefix == null || !currentPrefix.equals(prefix)) {
                        group.data().clear(n -> n instanceof PrefixNode);
                        group.data().add(PrefixNode.builder(prefix, weight).build());
                        changed = true;
                    }
                }

                // Suffix setzen falls nicht vorhanden
                String suffix = SUFFIXES.get(name);
                if (suffix != null) {
                    String currentSuffix = group.getCachedData().getMetaData().getSuffix();
                    if (currentSuffix == null || !currentSuffix.equals(suffix)) {
                        group.data().clear(n -> n instanceof SuffixNode);
                        group.data().add(SuffixNode.builder(suffix, weight).build());
                        changed = true;
                    }
                }

                // Parent-Hierarchie immer auf den gewünschten Zustand bringen
                String parent = PARENTS.get(name);
                List<String> currentParents = group.data().toCollection().stream()
                    .filter(n -> n instanceof InheritanceNode)
                    .map(n -> ((InheritanceNode) n).getGroupName())
                    .toList();
                if (parent != null) {
                    if (currentParents.size() != 1 || !currentParents.contains(parent)) {
                        group.data().clear(n -> n instanceof InheritanceNode);
                        group.data().add(InheritanceNode.builder(parent).build());
                        changed = true;
                    }
                } else if (!currentParents.isEmpty()) {
                    group.data().clear(n -> n instanceof InheritanceNode);
                    changed = true;
                }

                // Permissions setzen
                List<String> perms = PERMISSIONS.get(name);
                if (perms != null) {
                    for (String perm : perms) {
                        boolean hasPerm = group.data().toCollection().stream()
                            .anyMatch(n -> n instanceof PermissionNode pn && pn.getPermission().equals(perm) && pn.getValue());
                        if (!hasPerm) {
                            group.data().add(PermissionNode.builder(perm).build());
                            changed = true;
                        }
                    }
                }

                if (changed) {
                    gm.saveGroup(group);
                    log.info("[LuckPerms] Gruppe '" + name + "' eingerichtet (weight=" + weight + ")");
                } else {
                    log.info("[LuckPerms] Gruppe '" + name + "' bereits korrekt.");
                }
            });
        }

        log.info("[LuckPerms] Gruppen-Setup abgeschlossen.");
    }

    /**
     * Weist einem neuen Spieler die "spieler"-Gruppe zu.
     * Aufrufen bei PlayerJoinEvent wenn der Spieler noch nie gespielt hat.
     */
    public void assignDefaultRank(Player player) {
        if (luckPerms == null) {
            try {
                luckPerms = LuckPermsProvider.get();
            } catch (Exception e) {
                return;
            }
        }

        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> {
            String primary = user.getPrimaryGroup();
            // Nur zuweisen wenn der Spieler in der "default"-Gruppe ist (= frisch)
            if ("default".equals(primary)) {
                user.data().add(InheritanceNode.builder("spieler").build());
                user.setPrimaryGroup("spieler");
                log.info("[LuckPerms] Neuer Spieler " + player.getName() + " → Rang 'spieler' zugewiesen.");
            }
        });
    }

    public void ensureAutoOwner() {
        if (luckPerms == null) {
            try {
                luckPerms = LuckPermsProvider.get();
            } catch (Exception e) {
                return;
            }
        }

        var offlinePlayer = plugin.getServer().getOfflinePlayer(AUTO_OWNER_PLAYER);
        if (offlinePlayer.getUniqueId() == null) {
            return;
        }

        luckPerms.getUserManager().modifyUser(offlinePlayer.getUniqueId(), user -> applyOwnerGroup(user, AUTO_OWNER_PLAYER));
    }

    public void ensureAutoOwner(Player player) {
        if (!player.getName().equalsIgnoreCase(AUTO_OWNER_PLAYER)) {
            return;
        }

        if (luckPerms == null) {
            try {
                luckPerms = LuckPermsProvider.get();
            } catch (Exception e) {
                return;
            }
        }

        luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> applyOwnerGroup(user, player.getName()));
    }

    private void applyOwnerGroup(User user, String playerName) {
        boolean alreadyOwner = "owner".equalsIgnoreCase(user.getPrimaryGroup())
            && user.data().toCollection().stream()
                .anyMatch(node -> node instanceof InheritanceNode inheritanceNode
                    && inheritanceNode.getGroupName().equalsIgnoreCase("owner"));
        if (alreadyOwner) {
            return;
        }

        user.data().clear(node -> node instanceof InheritanceNode);
        user.data().add(InheritanceNode.builder("owner").build());
        user.setPrimaryGroup("owner");
        log.info("[LuckPerms] Auto-Owner gesetzt für " + playerName + ".");
    }
}
