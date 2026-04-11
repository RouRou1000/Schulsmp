package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import de.coolemod.donut.gui.ClanGUI;
import de.coolemod.donut.managers.ClanManager;
import de.coolemod.donut.systems.Clan;
import de.coolemod.donut.systems.ClanPermission;
import de.coolemod.donut.systems.ClanPermissionState;
import de.coolemod.donut.systems.ClanRole;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ClanListener implements Listener {
    private final DonutPlugin plugin;
    private final ClanManager clanManager;
    private final ClanGUI clanGUI;
    private final NamespacedKey actionKey;
    private final NamespacedKey permissionKey;
    private final NamespacedKey targetKey;

    public ClanListener(DonutPlugin plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.clanGUI = new ClanGUI(plugin);
        this.actionKey = new NamespacedKey(plugin, "clan_action");
        this.permissionKey = new NamespacedKey(plugin, "clan_permission");
        this.targetKey = new NamespacedKey(plugin, "clan_target");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ClanGUI.ClanViewHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        String action = getString(clicked, actionKey);
        if (action == null) {
            return;
        }

        Clan clan = clanManager.getClanById(holder.getClanId());
        if (clan == null) {
            player.closeInventory();
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDieser Clan existiert nicht mehr.");
            return;
        }
        if (!clan.isMember(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage("§8┃ §c§lCLAN §8┃ §cDu bist nicht mehr Mitglied dieses Clans.");
            return;
        }

        switch (action) {
            case "open_main" -> clanGUI.openMain(player, clan);
            case "open_members" -> clanGUI.openMembers(player, clan);
            case "open_invites" -> {
                if (!clan.hasPermission(player.getUniqueId(), ClanPermission.INVITE_MEMBERS)) {
                    deny(player, "Dir fehlt das Recht zum Einladen.");
                    return;
                }
                clanGUI.openInvites(player, clan);
            }
            case "open_settings" -> clanGUI.openSettings(player, clan);
            case "open_home" -> clanGUI.openHome(player, clan);
            case "open_global_permissions" -> clanGUI.openGlobalPermissions(player, clan);
            case "open_member_permissions" -> {
                UUID targetId = getTargetId(clicked, holder.getTargetMember());
                if (targetId == null || !clan.isMember(targetId)) {
                    deny(player, "Mitglied nicht gefunden.");
                    return;
                }
                clanGUI.openMemberPermissions(player, clan, targetId);
            }
            case "toggle_pvp" -> {
                if (!clan.hasPermission(player.getUniqueId(), ClanPermission.MANAGE_SETTINGS)) {
                    deny(player, "Dir fehlt das Recht fuer Clan-Settings.");
                    return;
                }
                clan.setPvpEnabled(!clan.isPvpEnabled());
                clanManager.save();
                clanGUI.openSettings(player, clan);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            }
            case "teleport_clan_home" -> {
                if (clan.getClanHome() == null) {
                    deny(player, "Es ist kein Clan-Home gesetzt.");
                    return;
                }
                player.closeInventory();
                plugin.getHomeManager().startTeleport(player, clan.getClanHome(), "§eClan-Home");
            }
            case "set_clan_home" -> {
                if (!clan.hasPermission(player.getUniqueId(), ClanPermission.SET_CLAN_HOME)) {
                    deny(player, "Dir fehlt das Recht zum Setzen des Clan-Homes.");
                    return;
                }
                clan.setClanHome(player.getLocation());
                clanManager.save();
                player.sendMessage("§8┃ §a§lCLAN §8┃ §7Clan-Home wurde gesetzt.");
                clanGUI.openHome(player, clan);
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
            }
            case "toggle_default_permission" -> {
                if (!clan.hasPermission(player.getUniqueId(), ClanPermission.MANAGE_PERMISSIONS)) {
                    deny(player, "Dir fehlt das Recht zum Bearbeiten der Clan-Rechte.");
                    return;
                }
                ClanPermission permission = getPermission(clicked);
                if (permission == null) {
                    return;
                }
                clan.setDefaultPermission(permission, !clan.getDefaultPermission(permission));
                clanManager.save();
                clanGUI.openGlobalPermissions(player, clan);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            }
            case "cycle_member_permission" -> {
                if (!clan.hasPermission(player.getUniqueId(), ClanPermission.MANAGE_PERMISSIONS)) {
                    deny(player, "Dir fehlt das Recht zum Bearbeiten der Clan-Rechte.");
                    return;
                }
                UUID targetId = getTargetId(clicked, holder.getTargetMember());
                ClanPermission permission = getPermission(clicked);
                if (targetId == null || permission == null || !clan.isMember(targetId)) {
                    return;
                }
                if (clan.isOwner(targetId)) {
                    deny(player, "Der Owner hat immer alle Rechte.");
                    return;
                }
                ClanPermissionState nextState = clan.getMemberOverride(targetId, permission).next();
                clan.setMemberOverride(targetId, permission, nextState);
                clanManager.save();
                clanGUI.openMemberPermissions(player, clan, targetId);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            }
            case "cycle_role" -> {
                if (!clan.hasPermission(player.getUniqueId(), ClanPermission.MANAGE_ROLES)) {
                    deny(player, "Dir fehlt das Recht zum Verwalten von Rollen.");
                    return;
                }
                UUID targetId = getTargetId(clicked, holder.getTargetMember());
                if (targetId == null || !clan.isMember(targetId) || clan.isOwner(targetId)) {
                    deny(player, "Diese Rolle kann nicht geaendert werden.");
                    return;
                }
                ClanRole nextRole = clan.getRole(targetId).nextManageable();
                clan.setRole(targetId, nextRole);
                clanManager.save();
                clanGUI.openMemberPermissions(player, clan, targetId);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
            }
            case "invite_player" -> {
                if (!clan.hasPermission(player.getUniqueId(), ClanPermission.INVITE_MEMBERS)) {
                    deny(player, "Dir fehlt das Recht zum Einladen.");
                    return;
                }
                UUID targetId = getTargetId(clicked, holder.getTargetMember());
                if (targetId == null) {
                    deny(player, "Spieler nicht gefunden.");
                    return;
                }
                Player target = Bukkit.getPlayer(targetId);
                if (target == null || clanManager.isInClan(targetId)) {
                    deny(player, "Spieler nicht verfuegbar.");
                    clanGUI.openInvites(player, clan);
                    return;
                }
                if (!clanManager.addInvite(clan, targetId)) {
                    deny(player, "Dieser Spieler hat bereits eine Einladung.");
                    return;
                }
                player.sendMessage("§8┃ §a§lCLAN §8┃ §e" + target.getName() + " §7wurde eingeladen.");
                target.sendMessage("§8┃ §e§lCLAN §8┃ §7Du wurdest in §e" + clan.getName() + " §7eingeladen.");
                target.sendMessage("§8┃ §7Nutze §e/clan accept " + clan.getName() + " §7oder oeffne §e/clan§7.");
                clanGUI.openInvites(player, clan);
            }
            case "toggle_clan_chat" -> {
                boolean enabled = clanManager.toggleClanChat(player.getUniqueId());
                player.sendMessage("§8┃ §d§lCLAN CHAT §8┃ §7Status: " + (enabled ? "§aAN" : "§cAUS"));
                clanGUI.openMain(player, clan);
            }
            case "kick_member" -> {
                if (!clan.hasPermission(player.getUniqueId(), ClanPermission.KICK_MEMBERS)) {
                    deny(player, "Dir fehlt das Recht zum Entfernen von Mitgliedern.");
                    return;
                }
                UUID targetId = getTargetId(clicked, holder.getTargetMember());
                if (targetId == null || !clan.isMember(targetId) || clan.isOwner(targetId)) {
                    deny(player, "Dieses Mitglied kann nicht entfernt werden.");
                    return;
                }
                String targetName = clanManager.getPlayerName(targetId);
                if (clanManager.kickMember(clan, targetId)) {
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null) {
                        target.sendMessage("§8┃ §c§lCLAN §8┃ §7Du wurdest aus §e" + clan.getName() + " §7entfernt.");
                    }
                    player.sendMessage("§8┃ §a§lCLAN §8┃ §e" + targetName + " §7wurde entfernt.");
                    clanGUI.openMembers(player, clan);
                }
            }
            case "leave_clan" -> {
                if (clan.isOwner(player.getUniqueId())) {
                    deny(player, "Owner muessen den Clan aufloesen.");
                    return;
                }
                if (clanManager.leaveClan(player.getUniqueId())) {
                    player.closeInventory();
                    player.sendMessage("§8┃ §a§lCLAN §8┃ §7Du hast §e" + clan.getName() + " §7verlassen.");
                }
            }
            case "disband_clan" -> {
                if (!clan.isOwner(player.getUniqueId())) {
                    deny(player, "Nur der Owner kann den Clan aufloesen.");
                    return;
                }
                List<UUID> members = new ArrayList<>(clan.getMembers());
                String clanName = clan.getName();
                clanManager.disbandClan(clan);
                player.closeInventory();
                for (UUID memberId : members) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        member.sendMessage("§8┃ §c§lCLAN §8┃ §7Der Clan §e" + clanName + " §7wurde aufgeloest.");
                    }
                }
            }
            case "close" -> player.closeInventory();
            case "noop" -> player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, 1.0f);
            default -> {
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ClanGUI.ClanViewHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        Clan attackerClan = clanManager.getClan(attacker.getUniqueId());
        Clan victimClan = clanManager.getClan(victim.getUniqueId());
        if (attackerClan == null || victimClan == null || !attackerClan.getId().equals(victimClan.getId())) {
            return;
        }

        if (attackerClan.isPvpEnabled()) {
            return;
        }

        event.setCancelled(true);
        attacker.sendMessage("§8┃ §e§lCLAN §8┃ §7Clan-PvP ist deaktiviert.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClanChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!clanManager.isClanChatEnabled(player.getUniqueId())) {
            return;
        }

        Clan clan = clanManager.getClan(player.getUniqueId());
        if (clan == null) {
            clanManager.setClanChatEnabled(player.getUniqueId(), false);
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> sendClanChatMessage(player, clan, message));
    }

    private Player resolveAttacker(Entity entity) {
        if (entity instanceof Player player) {
            return player;
        }
        if (entity instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private ClanPermission getPermission(ItemStack item) {
        String permissionRaw = getString(item, permissionKey);
        if (permissionRaw == null) {
            return null;
        }
        try {
            return ClanPermission.valueOf(permissionRaw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private UUID getTargetId(ItemStack item, UUID fallback) {
        String targetRaw = getString(item, targetKey);
        if (targetRaw == null) {
            return fallback;
        }
        try {
            return UUID.fromString(targetRaw);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private String getString(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private void deny(Player player, String message) {
        player.sendMessage("§8┃ §c§lCLAN §8┃ §c" + message);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
    }

    private void sendClanChatMessage(Player sender, Clan clan, String message) {
        String senderName = plugin.getScoreboardManager() != null
            ? plugin.getScoreboardManager().getChatFormat(sender)
            : "§7" + sender.getName();
        String formatted = "§8[§d§lCLAN§8] " + senderName + "§8: §f" + message;
        for (UUID memberId : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }
    }
}