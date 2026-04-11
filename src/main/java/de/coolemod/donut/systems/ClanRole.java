package de.coolemod.donut.systems;

import org.bukkit.Material;

public enum ClanRole {
    OWNER("Owner", Material.NETHER_STAR),
    CO_OWNER("Co-Owner", Material.DIAMOND),
    MOD("Mod", Material.IRON_SWORD),
    MEMBER("Member", Material.PLAYER_HEAD);

    private final String displayName;
    private final Material icon;

    ClanRole(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public ClanRole nextManageable() {
        return switch (this) {
            case CO_OWNER -> MOD;
            case MOD -> MEMBER;
            case MEMBER, OWNER -> CO_OWNER;
        };
    }
}