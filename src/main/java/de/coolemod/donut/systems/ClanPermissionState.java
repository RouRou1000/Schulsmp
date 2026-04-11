package de.coolemod.donut.systems;

public enum ClanPermissionState {
    INHERIT("§7Erbt global"),
    ALLOW("§aErlaubt"),
    DENY("§cVerweigert");

    private final String displayName;

    ClanPermissionState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ClanPermissionState next() {
        return switch (this) {
            case INHERIT -> ALLOW;
            case ALLOW -> DENY;
            case DENY -> INHERIT;
        };
    }
}