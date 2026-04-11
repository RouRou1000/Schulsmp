package de.coolemod.donut.gui;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.managers.ClanManager;
import de.coolemod.donut.systems.Clan;
import de.coolemod.donut.systems.ClanPermission;
import de.coolemod.donut.systems.ClanPermissionState;
import de.coolemod.donut.systems.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ClanGUI {
    private static final int[] MEMBER_SLOTS = {
        10, 11, 12, 13, 14, 15, 16,
        19, 20, 21, 22, 23, 24, 25,
        28, 29, 30, 31, 32, 33, 34,
        37, 38, 39, 40, 41, 42, 43
    };
    private static final int[] PERMISSION_SLOTS = {19, 20, 21, 23, 24, 25};

    private final DonutPlugin plugin;
    private final ClanManager clanManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey permissionKey;
    private final NamespacedKey targetKey;

    public ClanGUI(DonutPlugin plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.actionKey = new NamespacedKey(plugin, "clan_action");
        this.permissionKey = new NamespacedKey(plugin, "clan_permission");
        this.targetKey = new NamespacedKey(plugin, "clan_target");
    }

    public void openMain(Player player, Clan clan) {
        ClanViewHolder holder = new ClanViewHolder(ViewType.MAIN, clan.getId(), null);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§6§lClan §8| §e" + clan.getName());
        holder.setInventory(inventory);
        GUIUtils.fillBorders(inventory, plugin);

        inventory.setItem(4, createInfoItem(player, clan));
        inventory.setItem(19, actionItem(Material.PLAYER_HEAD, "§e§lMitglieder", Arrays.asList(
            "§8────────────────",
            "§7Mitglieder§8: §a" + clan.getMembers().size(),
            "§7Rollen und Einzelrechte",
            "§8────────────────"
        ), "open_members"));
        inventory.setItem(21, actionItem(Material.PAPER, "§a§lEinladungen", Arrays.asList(
            "§8────────────────",
            "§7Lade Spieler direkt",
            "§7ueber die GUI ein.",
            "§8────────────────"
        ), "open_invites"));
        inventory.setItem(23, actionItem(Material.COMPARATOR, "§6§lSettings", Arrays.asList(
            "§8────────────────",
            "§7Clan-PvP§8: " + boolText(clan.isPvpEnabled()),
            "§7Allgemeine Einstellungen",
            "§8────────────────"
        ), "open_settings"));
        inventory.setItem(25, actionItem(Material.RECOVERY_COMPASS, "§b§lClan-Home", Arrays.asList(
            "§8────────────────",
            clan.getClanHome() == null ? "§7Home§8: §cNicht gesetzt" : "§7Home§8: §aGesetzt",
            "§7Teleport und Verwaltung",
            "§8────────────────"
        ), "open_home"));
        inventory.setItem(31, actionItem(Material.WRITABLE_BOOK, "§b§lRechte", Arrays.asList(
            "§8────────────────",
            "§7Globale Rechte fuer den Clan",
            "§7plus individuelle Overrides",
            "§8────────────────"
        ), "open_global_permissions"));
        inventory.setItem(33, actionItem(clanManager.isClanChatEnabled(player.getUniqueId()) ? Material.LIME_DYE : Material.GRAY_DYE, "§d§lClan-Chat", Arrays.asList(
            "§8────────────────",
            "§7Status§8: " + (clanManager.isClanChatEnabled(player.getUniqueId()) ? "§aAktiv" : "§cInaktiv"),
            "§7/ c <nachricht> sendet direkt",
            "§8────────────────",
            "§eKlicke zum Umschalten"
        ), "toggle_clan_chat"));

        inventory.setItem(49, actionItem(Material.BARRIER,
            clan.isOwner(player.getUniqueId()) ? "§c§lClan aufloesen" : "§c§lClan verlassen",
            Arrays.asList(
                "§8────────────────",
                clan.isOwner(player.getUniqueId()) ? "§7Loest den Clan komplett auf" : "§7Verlaesst deinen Clan",
                "§8────────────────"
            ),
            clan.isOwner(player.getUniqueId()) ? "disband_clan" : "leave_clan"
        ));
        inventory.setItem(53, actionItem(Material.BARRIER, "§7Schliessen", List.of("§8────────────────", "§7GUI schliessen", "§8────────────────"), "close"));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openMembers(Player player, Clan clan) {
        ClanViewHolder holder = new ClanViewHolder(ViewType.MEMBERS, clan.getId(), null);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§6§lClan-Mitglieder");
        holder.setInventory(inventory);
        GUIUtils.fillBorders(inventory, plugin);

        List<UUID> members = new ArrayList<>(clan.getMembers());
        members.sort(Comparator
            .comparing((UUID memberId) -> !clan.isOwner(memberId))
            .thenComparing(memberId -> clan.getRole(memberId).ordinal())
            .thenComparing(clanManager::getPlayerName, String.CASE_INSENSITIVE_ORDER));

        int index = 0;
        for (UUID memberId : members) {
            if (index >= MEMBER_SLOTS.length) {
                break;
            }
            inventory.setItem(MEMBER_SLOTS[index++], memberHead(clan, memberId));
        }

        inventory.setItem(45, actionItem(Material.ARROW, "§eZurueck", List.of("§8────────────────", "§7Zur Clan-Uebersicht", "§8────────────────"), "open_main"));
        inventory.setItem(49, actionItem(Material.PAPER, "§a§lEinladungen", List.of("§8────────────────", "§7Spieler einladen", "§8────────────────"), "open_invites"));
        inventory.setItem(53, actionItem(Material.BARRIER, "§7Schliessen", List.of("§8────────────────", "§7GUI schliessen", "§8────────────────"), "close"));
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.1f);
    }

    public void openInvites(Player player, Clan clan) {
        ClanViewHolder holder = new ClanViewHolder(ViewType.INVITES, clan.getId(), null);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§6§lClan-Einladungen");
        holder.setInventory(inventory);
        GUIUtils.fillBorders(inventory, plugin);

        List<Player> invitable = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
            .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
            .filter(target -> !clanManager.isInClan(target.getUniqueId()))
            .toList());
        invitable.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        if (invitable.isEmpty()) {
            inventory.setItem(22, actionItem(Material.GRAY_DYE, "§7Keine Spieler verfuegbar", List.of(
                "§8────────────────",
                "§7Aktuell gibt es niemanden",
                "§7zum direkten Einladen.",
                "§8────────────────"
            ), "noop"));
        } else {
            int index = 0;
            for (Player target : invitable) {
                if (index >= MEMBER_SLOTS.length) {
                    break;
                }
                inventory.setItem(MEMBER_SLOTS[index++], inviteHead(target));
            }
        }

        inventory.setItem(45, actionItem(Material.ARROW, "§eZurueck", List.of("§8────────────────", "§7Zur Clan-Uebersicht", "§8────────────────"), "open_main"));
        inventory.setItem(53, actionItem(Material.BARRIER, "§7Schliessen", List.of("§8────────────────", "§7GUI schliessen", "§8────────────────"), "close"));
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.1f);
    }

    public void openSettings(Player player, Clan clan) {
        ClanViewHolder holder = new ClanViewHolder(ViewType.SETTINGS, clan.getId(), null);
        Inventory inventory = Bukkit.createInventory(holder, 45, "§6§lClan-Settings");
        holder.setInventory(inventory);
        GUIUtils.fillBorders(inventory, plugin);

        inventory.setItem(22, actionItem(Material.COMPARATOR, "§6§lClan-PvP", Arrays.asList(
            "§8────────────────",
            "§7Status§8: " + boolText(clan.isPvpEnabled()),
            "§7Wenn deaktiviert, koennen",
            "§7Clan-Mitglieder sich nicht hitten.",
            "§8────────────────",
            "§eKlicke zum Umschalten"
        ), "toggle_pvp"));
        inventory.setItem(36, actionItem(Material.ARROW, "§eZurueck", List.of("§8────────────────", "§7Zur Clan-Uebersicht", "§8────────────────"), "open_main"));
        inventory.setItem(40, actionItem(Material.BARRIER, "§7Schliessen", List.of("§8────────────────", "§7GUI schliessen", "§8────────────────"), "close"));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.1f);
    }

    public void openHome(Player player, Clan clan) {
        ClanViewHolder holder = new ClanViewHolder(ViewType.HOME, clan.getId(), null);
        Inventory inventory = Bukkit.createInventory(holder, 45, "§6§lClan-Home");
        holder.setInventory(inventory);
        GUIUtils.fillBorders(inventory, plugin);

        inventory.setItem(13, homeInfoItem(clan));
        inventory.setItem(20, actionItem(Material.ENDER_PEARL, "§a§lTeleportieren", Arrays.asList(
            "§8────────────────",
            clan.getClanHome() == null ? "§cKein Clan-Home gesetzt" : "§7Teleport mit Countdown",
            "§8────────────────",
            "§eKlicke zum Teleport"
        ), "teleport_clan_home"));
        inventory.setItem(24, actionItem(Material.RECOVERY_COMPASS, "§6§lClan-Home setzen", Arrays.asList(
            "§8────────────────",
            "§7Setzt den aktuellen Ort",
            "§7als neuen Clan-Home.",
            "§8────────────────",
            "§eKlicke zum Setzen"
        ), "set_clan_home"));
        inventory.setItem(36, actionItem(Material.ARROW, "§eZurueck", List.of("§8────────────────", "§7Zur Clan-Uebersicht", "§8────────────────"), "open_main"));
        inventory.setItem(40, actionItem(Material.BARRIER, "§7Schliessen", List.of("§8────────────────", "§7GUI schliessen", "§8────────────────"), "close"));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.1f);
    }

    public void openGlobalPermissions(Player player, Clan clan) {
        ClanViewHolder holder = new ClanViewHolder(ViewType.GLOBAL_PERMISSIONS, clan.getId(), null);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§6§lClan-Rechte");
        holder.setInventory(inventory);
        GUIUtils.fillBorders(inventory, plugin);

        for (int index = 0; index < ClanPermission.values().length && index < PERMISSION_SLOTS.length; index++) {
            inventory.setItem(PERMISSION_SLOTS[index], globalPermissionItem(clan, ClanPermission.values()[index]));
        }

        inventory.setItem(45, actionItem(Material.ARROW, "§eZurueck", List.of("§8────────────────", "§7Zur Clan-Uebersicht", "§8────────────────"), "open_main"));
        inventory.setItem(53, actionItem(Material.BARRIER, "§7Schliessen", List.of("§8────────────────", "§7GUI schliessen", "§8────────────────"), "close"));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.1f);
    }

    public void openMemberPermissions(Player player, Clan clan, UUID targetId) {
        ClanViewHolder holder = new ClanViewHolder(ViewType.MEMBER_PERMISSIONS, clan.getId(), targetId);
        Inventory inventory = Bukkit.createInventory(holder, 54, "§6§lMitglied verwalten");
        holder.setInventory(inventory);
        GUIUtils.fillBorders(inventory, plugin);

        inventory.setItem(4, memberInfoHead(clan, targetId));
        for (int index = 0; index < ClanPermission.values().length && index < PERMISSION_SLOTS.length; index++) {
            inventory.setItem(PERMISSION_SLOTS[index], memberPermissionItem(clan, targetId, ClanPermission.values()[index]));
        }
        inventory.setItem(31, roleItem(clan, targetId));
        if (!clan.isOwner(targetId)) {
            inventory.setItem(49, kickItem(targetId));
        }
        inventory.setItem(45, actionItem(Material.ARROW, "§eZurueck", List.of("§8────────────────", "§7Zur Mitgliederliste", "§8────────────────"), "open_members"));
        inventory.setItem(53, actionItem(Material.BARRIER, "§7Schliessen", List.of("§8────────────────", "§7GUI schliessen", "§8────────────────"), "close"));

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.1f);
    }

    private ItemStack createInfoItem(Player player, Clan clan) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6§l" + clan.getName());
        meta.setLore(Arrays.asList(
            "§8────────────────",
            "§7Owner§8: §e" + clanManager.getPlayerName(clan.getOwner()),
            "§7Deine Rolle§8: §b" + clan.getRole(player.getUniqueId()).getDisplayName(),
            "§7Mitglieder§8: §a" + clan.getMembers().size(),
            "§7Clan-PvP§8: " + boolText(clan.isPvpEnabled()),
            "§7Home§8: " + (clan.getClanHome() == null ? "§cNicht gesetzt" : "§aGesetzt"),
            "§7Clan-Chat§8: " + (clanManager.isClanChatEnabled(player.getUniqueId()) ? "§aAktiv" : "§cInaktiv"),
            "§7Deine Rechte§8: §b" + countGrantedPermissions(clan, player.getUniqueId()) + "§7/§b" + ClanPermission.values().length,
            "§8────────────────"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack globalPermissionItem(Clan clan, ClanPermission permission) {
        ItemStack item = new ItemStack(clan.getDefaultPermission(permission) ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§l" + permission.getDisplayName());
        meta.setLore(Arrays.asList(
            "§8────────────────",
            "§7" + permission.getDescription(),
            "§7Global§8: " + boolText(clan.getDefaultPermission(permission)),
            "§8────────────────",
            "§eKlicke zum Umschalten"
        ));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "toggle_default_permission");
        meta.getPersistentDataContainer().set(permissionKey, PersistentDataType.STRING, permission.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack memberPermissionItem(Clan clan, UUID targetId, ClanPermission permission) {
        ClanPermissionState state = clan.getMemberOverride(targetId, permission);
        ItemStack item = new ItemStack(switch (state) {
            case ALLOW -> Material.LIME_DYE;
            case DENY -> Material.RED_DYE;
            case INHERIT -> Material.GRAY_DYE;
        });
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§e§l" + permission.getDisplayName());
        meta.setLore(Arrays.asList(
            "§8────────────────",
            "§7" + permission.getDescription(),
            "§7Override§8: " + state.getDisplayName(),
            "§7Effektiv§8: " + boolText(clan.hasPermission(targetId, permission)),
            "§8────────────────",
            clan.isOwner(targetId) ? "§cOwner hat immer alle Rechte" : "§eKlicke zum Wechseln"
        ));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "cycle_member_permission");
        meta.getPersistentDataContainer().set(permissionKey, PersistentDataType.STRING, permission.name());
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, targetId.toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack roleItem(Clan clan, UUID targetId) {
        ClanRole role = clan.getRole(targetId);
        Material material = clan.isOwner(targetId) ? Material.NETHER_STAR : Material.NAME_TAG;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§d§lRolle");
        meta.setLore(Arrays.asList(
            "§8────────────────",
            "§7Aktuell§8: §b" + role.getDisplayName(),
            clan.isOwner(targetId) ? "§cDer Owner kann nicht veraendert werden" : "§7Naechste Rolle§8: §e" + role.nextManageable().getDisplayName(),
            "§8────────────────",
            clan.isOwner(targetId) ? "§7Nur Anzeige" : "§eKlicke zum Wechseln"
        ));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "cycle_role");
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, targetId.toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack memberHead(Clan clan, UUID memberId) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberId);
        meta.setOwningPlayer(offlinePlayer);
        meta.setDisplayName((clan.isOwner(memberId) ? "§6§l" : "§e") + clanManager.getPlayerName(memberId));
        meta.setLore(Arrays.asList(
            "§8────────────────",
            "§7Rolle§8: §b" + clan.getRole(memberId).getDisplayName(),
            "§7Rechte§8: §b" + countGrantedPermissions(clan, memberId) + "§7/§b" + ClanPermission.values().length,
            "§8────────────────",
            "§eKlicke fuer Details"
        ));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_member_permissions");
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, memberId.toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack memberInfoHead(Clan clan, UUID targetId) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(targetId));
        meta.setDisplayName((clan.isOwner(targetId) ? "§6§l" : "§e") + clanManager.getPlayerName(targetId));
        meta.setLore(Arrays.asList(
            "§8────────────────",
            "§7Rolle§8: §b" + clan.getRole(targetId).getDisplayName(),
            "§7Rechte aktiv§8: §b" + countGrantedPermissions(clan, targetId) + "§7/§b" + ClanPermission.values().length,
            "§8────────────────"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack inviteHead(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(target);
        meta.setDisplayName("§a§l" + target.getName());
        meta.setLore(Arrays.asList(
            "§8────────────────",
            "§7Status§8: §aOnline",
            "§7Klicke zum Einladen",
            "§8────────────────"
        ));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "invite_player");
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack homeInfoItem(Clan clan) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b§lHome-Status");
        Location home = clan.getClanHome();
        List<String> lore = new ArrayList<>();
        lore.add("§8────────────────");
        if (home == null || home.getWorld() == null) {
            lore.add("§7Status§8: §cNicht gesetzt");
        } else {
            lore.add("§7Status§8: §aGesetzt");
            lore.add("§7Welt§8: §e" + home.getWorld().getName());
            lore.add("§7Koords§8: §f" + home.getBlockX() + " " + home.getBlockY() + " " + home.getBlockZ());
        }
        lore.add("§8────────────────");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack kickItem(UUID targetId) {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§c§lMitglied entfernen");
        meta.setLore(Arrays.asList(
            "§8────────────────",
            "§7Entfernt dieses Mitglied",
            "§7aus dem Clan.",
            "§8────────────────",
            "§eKlicke zum Entfernen"
        ));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "kick_member");
        meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, targetId.toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack actionItem(Material material, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(new ArrayList<>(lore));
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private int countGrantedPermissions(Clan clan, UUID playerId) {
        int count = 0;
        for (ClanPermission permission : ClanPermission.values()) {
            if (clan.hasPermission(playerId, permission)) {
                count++;
            }
        }
        return count;
    }

    private String boolText(boolean enabled) {
        return enabled ? "§aAN" : "§cAUS";
    }

    public enum ViewType {
        MAIN,
        MEMBERS,
        INVITES,
        SETTINGS,
        HOME,
        GLOBAL_PERMISSIONS,
        MEMBER_PERMISSIONS
    }

    public static final class ClanViewHolder implements InventoryHolder {
        private final ViewType viewType;
        private final String clanId;
        private final UUID targetMember;
        private Inventory inventory;

        public ClanViewHolder(ViewType viewType, String clanId, UUID targetMember) {
            this.viewType = viewType;
            this.clanId = clanId;
            this.targetMember = targetMember;
        }

        public ViewType getViewType() {
            return viewType;
        }

        public String getClanId() {
            return clanId;
        }

        public UUID getTargetMember() {
            return targetMember;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}