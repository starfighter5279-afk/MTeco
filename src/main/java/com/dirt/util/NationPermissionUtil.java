package com.dirt.util;

import com.dirt.DirtEconomy;
import com.dirt.config.RolesConfig;
import com.dirt.data.CustomRole;
import com.dirt.data.NationData;
import com.dirt.data.NationPermission;
import com.dirt.data.RoleMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class NationPermissionUtil {

    public static boolean hasPermission(DirtEconomy plugin, NationData nation, UUID playerUUID, NationPermission permission) {
        if (nation == null || playerUUID == null) return false;
        if (playerUUID.equals(nation.getPresidentUUID())) return true;

        if (plugin.getRolesConfig().isCustomGovernmentEnabled()) {
            for (CustomRole role : nation.getCustomRoles()) {
                if (role.getMemberUUIDs().contains(playerUUID)) {
                    if (role.getRoleMode() == RoleMode.LEADER && plugin.getRolesConfig().isLeaderModeEnabled()) {
                        return true;
                    }
                    if (role.getPermissions().contains(permission)) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            return hasLegacyPermission(plugin, nation, playerUUID, permission);
        }
    }

    private static boolean hasLegacyPermission(DirtEconomy plugin, NationData nation, UUID playerUUID, NationPermission permission) {
        RolesConfig rc = plugin.getRolesConfig();
        boolean isVP = rc.isVicePresidentEnabled() && playerUUID.equals(nation.getVicePresidentUUID());
        boolean isTreasurer = rc.isTreasurerEnabled() && playerUUID.equals(nation.getTreasurerUUID());
        boolean isSecurityHead = rc.isSecurityHeadEnabled() && playerUUID.equals(nation.getSecurityHeadUUID());

        switch (permission) {
            case MANAGE_TREASURY:
                return isTreasurer && rc.canTreasurerManageTreasury();
            case VIEW_TREASURY:
                if (isTreasurer) return true;
                return isVP && rc.canVpViewTreasury();
            case MANAGE_PROPERTIES:
                return isTreasurer && rc.canTreasurerApproveProperties();
            case MANAGE_REGIONS:
                return isVP && rc.canVpManageRegions();
            case MANAGE_WAR:
                if (isVP && rc.canVpManageWar()) return true;
                return isSecurityHead && rc.canSecurityManageWars();
            case MANAGE_ANNOUNCEMENTS:
                return isVP && rc.canVpManageAnnouncements();
            case MANAGE_ENFORCERS:
                return isSecurityHead;
            case MANAGE_CRIME:
            case MANAGE_JAILS:
                return isSecurityHead;
            case MANAGE_LAWS:
                return isVP;
            case MANAGE_MINTERS:
                return isTreasurer;
            case USE_MINTERS:
                return isTreasurer;
            case MANAGE_CONSERVATION:
                return isTreasurer;
            case INVITE_MEMBERS:
            case KICK_MEMBERS:
            default:
                return false;
        }
    }

    public static String getRoleDisplayName(DirtEconomy plugin, NationData nation, UUID playerUUID) {
        if (nation == null) return "\u00a77Member";
        if (playerUUID.equals(nation.getPresidentUUID())) return "\u00a76President";

        if (plugin.getRolesConfig().isCustomGovernmentEnabled()) {
            StringBuilder roles = new StringBuilder();
            for (CustomRole role : nation.getCustomRoles()) {
                if (role.getMemberUUIDs().contains(playerUUID)) {
                    if (roles.length() > 0) roles.append("\u00a77, ");
                    roles.append(role.getColorCode()).append(role.getName());
                }
            }
            return roles.length() > 0 ? roles.toString() : "\u00a77Citizen";
        } else {
            if (playerUUID.equals(nation.getVicePresidentUUID())) return "\u00a79Vice President";
            if (playerUUID.equals(nation.getTreasurerUUID())) return "\u00a7eTreasurer";
            if (playerUUID.equals(nation.getSecurityHeadUUID())) return "\u00a7cSecurity Head";
            return "\u00a77Member";
        }
    }

    public static Set<NationPermission> getPlayerPermissions(DirtEconomy plugin, NationData nation, UUID playerUUID) {
        if (nation == null) return Set.of();
        if (playerUUID.equals(nation.getPresidentUUID())) return EnumSet.allOf(NationPermission.class);

        if (plugin.getRolesConfig().isCustomGovernmentEnabled()) {
            Set<NationPermission> perms = EnumSet.noneOf(NationPermission.class);
            for (CustomRole role : nation.getCustomRoles()) {
                if (role.getMemberUUIDs().contains(playerUUID)) {
                    if (role.getRoleMode() == RoleMode.LEADER && plugin.getRolesConfig().isLeaderModeEnabled()) {
                        return EnumSet.allOf(NationPermission.class);
                    }
                    perms.addAll(role.getPermissions());
                }
            }
            return perms;
        } else {
            Set<NationPermission> perms = EnumSet.noneOf(NationPermission.class);
            for (NationPermission p : NationPermission.values()) {
                if (hasLegacyPermission(plugin, nation, playerUUID, p)) {
                    perms.add(p);
                }
            }
            return perms;
        }
    }

    public static boolean requiresCouncilVote(DirtEconomy plugin, NationData nation, UUID playerUUID) {
        if (nation == null || playerUUID == null) return false;
        if (playerUUID.equals(nation.getPresidentUUID())) return false;

        RolesConfig rc = plugin.getRolesConfig();
        if (!rc.isCustomGovernmentEnabled() || !rc.isCouncilModeEnabled()) return false;

        boolean isLeader = false;
        boolean isCouncil = false;
        for (CustomRole role : nation.getCustomRoles()) {
            if (role.getMemberUUIDs().contains(playerUUID)) {
                if (role.getRoleMode() == RoleMode.LEADER) isLeader = true;
                if (role.getRoleMode() == RoleMode.COUNCIL) isCouncil = true;
            }
        }

        boolean nationHasCouncil = nation.getCustomRoles().stream()
                .anyMatch(r -> r.getRoleMode() == RoleMode.COUNCIL && !r.getMemberUUIDs().isEmpty());
        boolean nationHasLeader = nation.getCustomRoles().stream()
                .anyMatch(r -> r.getRoleMode() == RoleMode.LEADER && !r.getMemberUUIDs().isEmpty());

        if (isLeader && nationHasCouncil) return true;
        if (isCouncil && !nationHasLeader) return true;

        return false;
    }

    public static boolean executeOrVote(DirtEconomy plugin, NationData nation, UUID playerUUID,
                                         NationPermission permission, String description, Runnable action) {
        if (nation == null) return false;
        if (playerUUID.equals(nation.getPresidentUUID())) {
            action.run();
            return true;
        }

        RolesConfig rc = plugin.getRolesConfig();
        if (!rc.isCustomGovernmentEnabled() || !rc.isCouncilModeEnabled()) {
            action.run();
            return true;
        }

        boolean isLeader = false;
        boolean isCouncil = false;
        for (CustomRole role : nation.getCustomRoles()) {
            if (role.getMemberUUIDs().contains(playerUUID)) {
                if (role.getRoleMode() == RoleMode.LEADER) isLeader = true;
                if (role.getRoleMode() == RoleMode.COUNCIL) isCouncil = true;
            }
        }

        boolean nationHasCouncil = nation.getCustomRoles().stream()
                .anyMatch(r -> r.getRoleMode() == RoleMode.COUNCIL && !r.getMemberUUIDs().isEmpty());
        boolean nationHasLeader = nation.getCustomRoles().stream()
                .anyMatch(r -> r.getRoleMode() == RoleMode.LEADER && !r.getMemberUUIDs().isEmpty());

        boolean needsVote = (isLeader && nationHasCouncil) || (isCouncil && !nationHasLeader);

        if (needsVote) {
            Player p = Bukkit.getPlayer(playerUUID);
            plugin.getCouncilVoteManager().createVote(nation, playerUUID, permission, description, action, () -> {
                if (p != null && p.isOnline()) {
                    p.sendMessage("\u00a7cCouncil denied: \u00a7f" + description);
                }
            });
            if (p != null && p.isOnline()) {
                p.sendMessage("\u00a76[Council] \u00a77Submitted for council approval: \u00a7f" + description);
            }
            return false;
        }

        action.run();
        return true;
    }
}