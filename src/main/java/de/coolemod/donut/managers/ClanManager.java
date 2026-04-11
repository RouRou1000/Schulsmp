package de.coolemod.donut.managers;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.systems.Clan;
import de.coolemod.donut.systems.ClanPermission;
import de.coolemod.donut.systems.ClanPermissionState;
import de.coolemod.donut.systems.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClanManager {
    private final DonutPlugin plugin;
    private final Map<String, Clan> clansById = new LinkedHashMap<>();
    private final Map<UUID, String> playerClanIds = new HashMap<>();
    private final Map<UUID, Set<String>> pendingInvites = new HashMap<>();
    private final Set<UUID> clanChatEnabled = new LinkedHashSet<>();
    private final File dataFile;
    private FileConfiguration dataConfig;

    public ClanManager(DonutPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "clans.yml");
        load();
    }

    public synchronized void load() {
        clansById.clear();
        playerClanIds.clear();
        pendingInvites.clear();

        if (!dataFile.exists()) {
            dataConfig = new YamlConfiguration();
            return;
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection clansSection = dataConfig.getConfigurationSection("clans");
        if (clansSection == null) {
            return;
        }

        for (String clanId : clansSection.getKeys(false)) {
            try {
                String path = "clans." + clanId;
                String name = dataConfig.getString(path + ".name");
                String ownerRaw = dataConfig.getString(path + ".owner");
                if (name == null || ownerRaw == null) {
                    continue;
                }

                Clan clan = new Clan(clanId, name, UUID.fromString(ownerRaw));
                clan.setPvpEnabled(dataConfig.getBoolean(path + ".pvpEnabled", false));

                String homeWorldName = dataConfig.getString(path + ".home.world");
                if (homeWorldName != null) {
                    World homeWorld = Bukkit.getWorld(homeWorldName);
                    if (homeWorld != null) {
                        clan.setClanHome(new Location(
                            homeWorld,
                            dataConfig.getDouble(path + ".home.x"),
                            dataConfig.getDouble(path + ".home.y"),
                            dataConfig.getDouble(path + ".home.z"),
                            (float) dataConfig.getDouble(path + ".home.yaw"),
                            (float) dataConfig.getDouble(path + ".home.pitch")
                        ));
                    }
                }

                for (String memberRaw : dataConfig.getStringList(path + ".members")) {
                    try {
                        clan.addMember(UUID.fromString(memberRaw));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                clan.addMember(clan.getOwner());

                ConfigurationSection rolesSection = dataConfig.getConfigurationSection(path + ".memberRoles");
                if (rolesSection != null) {
                    for (String memberRaw : rolesSection.getKeys(false)) {
                        try {
                            UUID memberId = UUID.fromString(memberRaw);
                            String roleRaw = rolesSection.getString(memberRaw);
                            if (roleRaw != null) {
                                clan.setRole(memberId, ClanRole.valueOf(roleRaw));
                            }
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }

                ConfigurationSection defaultsSection = dataConfig.getConfigurationSection(path + ".defaultPermissions");
                if (defaultsSection != null) {
                    for (ClanPermission permission : ClanPermission.values()) {
                        if (defaultsSection.contains(permission.name())) {
                            clan.setDefaultPermission(permission, defaultsSection.getBoolean(permission.name()));
                        }
                    }
                }

                ConfigurationSection overridesSection = dataConfig.getConfigurationSection(path + ".memberOverrides");
                if (overridesSection != null) {
                    for (String memberRaw : overridesSection.getKeys(false)) {
                        UUID memberId;
                        try {
                            memberId = UUID.fromString(memberRaw);
                        } catch (IllegalArgumentException ignored) {
                            continue;
                        }
                        ConfigurationSection memberSection = overridesSection.getConfigurationSection(memberRaw);
                        if (memberSection == null) {
                            continue;
                        }
                        for (ClanPermission permission : ClanPermission.values()) {
                            String stateRaw = memberSection.getString(permission.name());
                            if (stateRaw == null) {
                                continue;
                            }
                            try {
                                clan.setMemberOverride(memberId, permission, ClanPermissionState.valueOf(stateRaw));
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                }

                clansById.put(clanId, clan);
                for (UUID memberId : clan.getMembers()) {
                    playerClanIds.put(memberId, clanId);
                }
            } catch (Exception exception) {
                plugin.getLogger().warning("[ClanSystem] Fehler beim Laden von Clan " + clanId + ": " + exception.getMessage());
            }
        }
    }

    public synchronized void save() {
        dataConfig = new YamlConfiguration();
        for (Clan clan : clansById.values()) {
            String path = "clans." + clan.getId();
            dataConfig.set(path + ".name", clan.getName());
            dataConfig.set(path + ".owner", clan.getOwner().toString());
            dataConfig.set(path + ".pvpEnabled", clan.isPvpEnabled());

            Location home = clan.getClanHome();
            if (home != null && home.getWorld() != null) {
                dataConfig.set(path + ".home.world", home.getWorld().getName());
                dataConfig.set(path + ".home.x", home.getX());
                dataConfig.set(path + ".home.y", home.getY());
                dataConfig.set(path + ".home.z", home.getZ());
                dataConfig.set(path + ".home.yaw", home.getYaw());
                dataConfig.set(path + ".home.pitch", home.getPitch());
            }

            List<String> members = new ArrayList<>();
            for (UUID memberId : clan.getMembers()) {
                members.add(memberId.toString());
            }
            dataConfig.set(path + ".members", members);

            for (UUID memberId : clan.getMembers()) {
                if (!clan.isOwner(memberId)) {
                    dataConfig.set(path + ".memberRoles." + memberId, clan.getRole(memberId).name());
                }
            }

            for (ClanPermission permission : ClanPermission.values()) {
                dataConfig.set(path + ".defaultPermissions." + permission.name(), clan.getDefaultPermission(permission));
            }

            for (UUID memberId : clan.getMembers()) {
                for (ClanPermission permission : ClanPermission.values()) {
                    ClanPermissionState state = clan.getMemberOverride(memberId, permission);
                    if (state != ClanPermissionState.INHERIT) {
                        dataConfig.set(path + ".memberOverrides." + memberId + "." + permission.name(), state.name());
                    }
                }
            }
        }

        try {
            File folder = dataFile.getParentFile();
            if (folder != null && !folder.exists()) {
                folder.mkdirs();
            }
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("[ClanSystem] Fehler beim Speichern: " + exception.getMessage());
        }
    }

    public synchronized Clan getClan(UUID playerId) {
        String clanId = playerClanIds.get(playerId);
        return clanId == null ? null : clansById.get(clanId);
    }

    public synchronized Clan getClanById(String clanId) {
        return clansById.get(clanId);
    }

    public synchronized Clan findClanByName(String name) {
        if (name == null) {
            return null;
        }
        for (Clan clan : clansById.values()) {
            if (clan.getName().equalsIgnoreCase(name)) {
                return clan;
            }
        }
        return null;
    }

    public synchronized boolean isInClan(UUID playerId) {
        return playerClanIds.containsKey(playerId);
    }

    public String validateClanName(UUID ownerId, String name) {
        if (isInClan(ownerId)) {
            return "§8┃ §c§lCLAN §8┃ §cDu bist bereits in einem Clan.";
        }

        String cleanName = name == null ? "" : name.trim();
        if (cleanName.length() < 3 || cleanName.length() > 16) {
            return "§8┃ §c§lCLAN §8┃ §cClan-Namen muessen 3 bis 16 Zeichen haben.";
        }
        if (!cleanName.matches("[\\p{L}0-9_-]+")) {
            return "§8┃ §c§lCLAN §8┃ §cNur Buchstaben, Zahlen, _ und - sind erlaubt.";
        }
        if (findClanByName(cleanName) != null) {
            return "§8┃ §c§lCLAN §8┃ §cDieser Clan-Name existiert bereits.";
        }
        return null;
    }

    public synchronized Clan createClan(UUID ownerId, String name) {
        String cleanName = name.trim();
        Clan clan = new Clan(UUID.randomUUID().toString(), cleanName, ownerId);
        clansById.put(clan.getId(), clan);
        playerClanIds.put(ownerId, clan.getId());
        save();
        return clan;
    }

    public synchronized boolean addInvite(Clan clan, UUID targetId) {
        if (clan == null || isInClan(targetId)) {
            return false;
        }
        return pendingInvites.computeIfAbsent(targetId, ignored -> new LinkedHashSet<>()).add(clan.getId());
    }

    public synchronized Clan acceptInvite(UUID playerId, String clanNameOrId) {
        if (isInClan(playerId)) {
            return null;
        }

        Clan clan = clansById.get(clanNameOrId);
        if (clan == null) {
            clan = findClanByName(clanNameOrId);
        }
        if (clan == null) {
            return null;
        }

        Set<String> invites = pendingInvites.getOrDefault(playerId, Collections.emptySet());
        if (!invites.contains(clan.getId())) {
            return null;
        }

        clan.addMember(playerId);
        playerClanIds.put(playerId, clan.getId());
        invites.remove(clan.getId());
        if (invites.isEmpty()) {
            pendingInvites.remove(playerId);
        }
        clanChatEnabled.remove(playerId);
        save();
        return clan;
    }

    public synchronized boolean leaveClan(UUID playerId) {
        Clan clan = getClan(playerId);
        if (clan == null || clan.isOwner(playerId)) {
            return false;
        }
        if (!clan.removeMember(playerId)) {
            return false;
        }
        playerClanIds.remove(playerId);
        clanChatEnabled.remove(playerId);
        save();
        return true;
    }

    public synchronized boolean kickMember(Clan clan, UUID targetId) {
        if (clan == null || !clan.removeMember(targetId)) {
            return false;
        }
        playerClanIds.remove(targetId);
        clanChatEnabled.remove(targetId);
        save();
        return true;
    }

    public synchronized void disbandClan(Clan clan) {
        if (clan == null) {
            return;
        }
        clansById.remove(clan.getId());
        for (UUID memberId : clan.getMembers()) {
            playerClanIds.remove(memberId);
            clanChatEnabled.remove(memberId);
        }
        pendingInvites.values().forEach(invites -> invites.remove(clan.getId()));
        pendingInvites.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        save();
    }

    public synchronized List<String> getInviteClanNames(UUID playerId) {
        List<String> result = new ArrayList<>();
        for (String clanId : pendingInvites.getOrDefault(playerId, Collections.emptySet())) {
            Clan clan = clansById.get(clanId);
            if (clan != null) {
                result.add(clan.getName());
            }
        }
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public String getPlayerName(UUID playerId) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : playerId.toString().substring(0, 8).toLowerCase(Locale.ROOT);
    }

    public synchronized boolean isClanChatEnabled(UUID playerId) {
        return clanChatEnabled.contains(playerId) && isInClan(playerId);
    }

    public synchronized boolean toggleClanChat(UUID playerId) {
        if (!isInClan(playerId)) {
            clanChatEnabled.remove(playerId);
            return false;
        }
        if (!clanChatEnabled.add(playerId)) {
            clanChatEnabled.remove(playerId);
            return false;
        }
        return true;
    }

    public synchronized void setClanChatEnabled(UUID playerId, boolean enabled) {
        if (!enabled || !isInClan(playerId)) {
            clanChatEnabled.remove(playerId);
            return;
        }
        clanChatEnabled.add(playerId);
    }

    public synchronized String getSidebarClanName(UUID playerId) {
        Clan clan = getClan(playerId);
        if (clan == null) {
            return "§7Kein Clan";
        }
        ClanRole role = clan.getRole(playerId);
        return "§e" + clan.getName() + " §8(§7" + role.getDisplayName() + "§8)";
    }
}