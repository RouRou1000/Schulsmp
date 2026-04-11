package de.coolemod.donut.systems;

import org.bukkit.Material;

public enum ClanPermission {
    INVITE_MEMBERS("Mitglieder einladen", "Spieler in den Clan einladen", Material.PAPER),
    KICK_MEMBERS("Mitglieder entfernen", "Spieler aus dem Clan entfernen", Material.IRON_SWORD),
    MANAGE_SETTINGS("Settings verwalten", "Clan-Settings wie PvP aendern", Material.COMPARATOR),
    MANAGE_PERMISSIONS("Rechte verwalten", "Globale und individuelle Rechte bearbeiten", Material.WRITABLE_BOOK),
    MANAGE_ROLES("Rollen verwalten", "Mitglieder befoerdern oder herabstufen", Material.NAME_TAG),
    SET_CLAN_HOME("Clan-Home setzen", "Den Clan-Homepunkt verwalten", Material.RECOVERY_COMPASS);

    private final String displayName;
    private final String description;
    private final Material icon;

    ClanPermission(String displayName, String description, Material icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Material getIcon() {
        return icon;
    }
}