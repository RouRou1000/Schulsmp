package de.coolemod.schulcore.managers;

import de.coolemod.schulcore.SchulCorePlugin;
import de.coolemod.schulcore.storage.DataManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.*;

/**
 * Verwaltet Spieler-Ränge mit Prefix, Farbe und Permissions.
 * Daten werden in ranks.yml gespeichert.
 */
public class RankManager {

    public enum Rank {
        OWNER("§4§lOwner", "§4", true, List.of()),
        MOD("§c§lMod", "§c", false, List.of(
                "schulcore.admin",
                "schulcore.crate.admin",
                "schulcore.packetcheck",
                "schulcore.rank",
                "schulcore.money",
                "schulcore.shards",
                "schulcore.gamemode",
                "schulcore.fly",
                "schulcore.vanish",
                "schulcore.tp",
                "schulcore.ban",
                "schulcore.kick",
                "schulcore.mute",
                "schulcore.bypass",
                "schulcore.build",
                "donut.admin",
                "donut.balance.others"
        )),
        BUILDER("§a§lBuilder", "§a", false, List.of(
                "schulcore.admin",
                "schulcore.crate.admin",
                "schulcore.packetcheck",
                "schulcore.rank",
                "schulcore.money",
                "schulcore.shards",
                "schulcore.gamemode",
                "schulcore.fly",
                "schulcore.vanish",
                "schulcore.tp",
                "schulcore.ban",
                "schulcore.kick",
                "schulcore.mute",
                "schulcore.bypass",
                "schulcore.build",
                "donut.admin",
                "donut.balance.others"
        )),
        SPIELER("§7Spieler", "§7", false, List.of());

        private final String prefix;
        private final String color;
        private final boolean isOp;
        private final List<String> permissions;

        Rank(String prefix, String color, boolean isOp, List<String> permissions) {
            this.prefix = prefix;
            this.color = color;
            this.isOp = isOp;
            this.permissions = permissions;
        }

        public String getPrefix() { return prefix; }
        public String getColor() { return color; }
        public boolean isOp() { return isOp; }
        public List<String> getPermissions() { return permissions; }
    }

    private final SchulCorePlugin plugin;
    private final DataManager data;
    private final Map<UUID, Rank> playerRanks = new HashMap<>();
    private final Map<UUID, PermissionAttachment> attachments = new HashMap<>();

    public RankManager(SchulCorePlugin plugin) {
        this.plugin = plugin;
        this.data = new DataManager(plugin.getDataFolder(), "ranks.yml");
        load();
    }

    private static final Map<String, Rank> DEFAULT_RANKS = Map.of(
            "rourou1000", Rank.OWNER
    );

    private void load() {
        if (!data.getConfig().contains("ranks")) return;
        for (String key : data.getConfig().getConfigurationSection("ranks").getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String rankName = data.getConfig().getString("ranks." + key);
                Rank rank = Rank.valueOf(rankName.toUpperCase());
                playerRanks.put(uuid, rank);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        for (Map.Entry<UUID, Rank> entry : playerRanks.entrySet()) {
            data.getConfig().set("ranks." + entry.getKey().toString(), entry.getValue().name());
        }
        data.save();
    }

    public Rank getRank(UUID uuid) {
        return playerRanks.getOrDefault(uuid, Rank.SPIELER);
    }

    public Rank getRank(Player player) {
        return getRank(player.getUniqueId());
    }

    public void setRank(UUID uuid, Rank rank) {
        if (rank == Rank.SPIELER) {
            playerRanks.remove(uuid);
        } else {
            playerRanks.put(uuid, rank);
        }
        save();

        // Live-Update falls online
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            applyPermissions(player);
            applyTabName(player);
        }
    }

    /**
     * Wendet die Rank-Permissions auf den Spieler an.
     * Wird bei Join und bei Rank-Änderung aufgerufen.
     */
    public void applyPermissions(Player player) {
        // Default-Rank zuweisen falls nötig
        if (!playerRanks.containsKey(player.getUniqueId())) {
            Rank defaultRank = DEFAULT_RANKS.get(player.getName().toLowerCase());
            if (defaultRank != null) {
                playerRanks.put(player.getUniqueId(), defaultRank);
                save();
            }
        }

        // Altes Attachment entfernen
        PermissionAttachment old = attachments.remove(player.getUniqueId());
        if (old != null) {
            try { player.removeAttachment(old); } catch (Exception ignored) {}
        }

        Rank rank = getRank(player);

        // Owner bekommt OP
        if (rank.isOp()) {
            if (!player.isOp()) player.setOp(true);
        } else {
            // Nicht-Owner: OP entfernen falls vorher Owner war
            // (nur wenn der Spieler durch das Rang-System OP bekommen hat)
        }

        if (rank.getPermissions().isEmpty() && !rank.isOp()) return;

        PermissionAttachment attachment = player.addAttachment(plugin);
        for (String perm : rank.getPermissions()) {
            attachment.setPermission(perm, true);
        }
        attachments.put(player.getUniqueId(), attachment);
    }

    /**
     * Setzt den farbigen Tab-List-Namen mit Rank-Prefix.
     */
    public void applyTabName(Player player) {
        Rank rank = getRank(player);
        if (rank == Rank.SPIELER) {
            player.setPlayerListName(rank.getColor() + player.getName());
        } else {
            player.setPlayerListName(rank.getPrefix() + " §8┃ " + rank.getColor() + player.getName());
        }
    }

    /**
     * Gibt den formatierten Chat-String zurück: [Prefix] Spielername
     */
    public String getChatPrefix(Player player) {
        Rank rank = getRank(player);
        if (rank == Rank.SPIELER) {
            return rank.getColor() + player.getName();
        }
        return rank.getPrefix() + " §8┃ " + rank.getColor() + player.getName();
    }

    /**
     * Cleanup bei Quit
     */
    public void removeAttachment(UUID uuid) {
        PermissionAttachment att = attachments.remove(uuid);
        if (att != null) {
            try {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) p.removeAttachment(att);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Alle Ränge, die nicht SPIELER sind
     */
    public Map<UUID, Rank> getAllRanks() {
        return Collections.unmodifiableMap(playerRanks);
    }
}
