package de.coolemod.donut.systems;

import org.bukkit.Location;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Clan {
    private final String id;
    private final String name;
    private final UUID owner;
    private boolean pvpEnabled;
    private Location clanHome;
    private final Set<UUID> members = new LinkedHashSet<>();
    private final EnumMap<ClanPermission, Boolean> defaultPermissions = new EnumMap<>(ClanPermission.class);
    private final Map<UUID, EnumMap<ClanPermission, ClanPermissionState>> memberOverrides = new HashMap<>();
    private final Map<UUID, ClanRole> memberRoles = new HashMap<>();

    public Clan(String id, String name, UUID owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.members.add(owner);
        this.pvpEnabled = false;
        for (ClanPermission permission : ClanPermission.values()) {
            defaultPermissions.put(permission, false);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public Location getClanHome() {
        return clanHome == null ? null : clanHome.clone();
    }

    public void setClanHome(Location clanHome) {
        this.clanHome = clanHome == null ? null : clanHome.clone();
    }

    public boolean isOwner(UUID playerId) {
        return owner.equals(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public boolean addMember(UUID playerId) {
        if (!members.add(playerId)) {
            return false;
        }
        memberRoles.put(playerId, ClanRole.MEMBER);
        return true;
    }

    public boolean removeMember(UUID playerId) {
        if (isOwner(playerId)) {
            return false;
        }
        memberOverrides.remove(playerId);
        memberRoles.remove(playerId);
        return members.remove(playerId);
    }

    public Set<UUID> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public ClanRole getRole(UUID playerId) {
        if (isOwner(playerId)) {
            return ClanRole.OWNER;
        }
        return memberRoles.getOrDefault(playerId, ClanRole.MEMBER);
    }

    public void setRole(UUID playerId, ClanRole role) {
        if (!isMember(playerId) || isOwner(playerId) || role == null || role == ClanRole.OWNER) {
            return;
        }
        if (role == ClanRole.MEMBER) {
            memberRoles.remove(playerId);
            return;
        }
        memberRoles.put(playerId, role);
    }

    public boolean getDefaultPermission(ClanPermission permission) {
        return defaultPermissions.getOrDefault(permission, false);
    }

    public void setDefaultPermission(ClanPermission permission, boolean allowed) {
        defaultPermissions.put(permission, allowed);
    }

    public ClanPermissionState getMemberOverride(UUID playerId, ClanPermission permission) {
        EnumMap<ClanPermission, ClanPermissionState> overrides = memberOverrides.get(playerId);
        if (overrides == null) {
            return ClanPermissionState.INHERIT;
        }
        return overrides.getOrDefault(permission, ClanPermissionState.INHERIT);
    }

    public void setMemberOverride(UUID playerId, ClanPermission permission, ClanPermissionState state) {
        if (!isMember(playerId) || isOwner(playerId)) {
            return;
        }

        if (state == ClanPermissionState.INHERIT) {
            EnumMap<ClanPermission, ClanPermissionState> overrides = memberOverrides.get(playerId);
            if (overrides == null) {
                return;
            }
            overrides.remove(permission);
            if (overrides.isEmpty()) {
                memberOverrides.remove(playerId);
            }
            return;
        }

        memberOverrides.computeIfAbsent(playerId, ignored -> new EnumMap<>(ClanPermission.class)).put(permission, state);
    }

    public boolean hasPermission(UUID playerId, ClanPermission permission) {
        if (isOwner(playerId)) {
            return true;
        }
        if (!isMember(playerId)) {
            return false;
        }

        ClanPermissionState override = getMemberOverride(playerId, permission);
        if (override == ClanPermissionState.ALLOW) {
            return true;
        }
        if (override == ClanPermissionState.DENY) {
            return false;
        }

        if (roleGrantsPermission(getRole(playerId), permission)) {
            return true;
        }
        return getDefaultPermission(permission);
    }

    private boolean roleGrantsPermission(ClanRole role, ClanPermission permission) {
        return switch (role) {
            case OWNER, CO_OWNER -> true;
            case MOD -> permission == ClanPermission.INVITE_MEMBERS || permission == ClanPermission.KICK_MEMBERS;
            case MEMBER -> false;
        };
    }
}