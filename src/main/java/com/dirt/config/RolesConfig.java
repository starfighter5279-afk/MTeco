package com.dirt.config;

import com.dirt.DirtEconomy;
import com.dirt.data.NationPermission;
import com.dirt.data.RoleMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class RolesConfig {

    private final DirtEconomy plugin;
    private FileConfiguration config;

    private boolean elitesEnabled;

    private boolean treasurerEnabled;
    private boolean treasurerTreasury;
    private boolean treasurerPropertyApprovals;

    private boolean vpEnabled;
    private boolean vpRegions;
    private boolean vpWar;
    private boolean vpTreasuryView;
    private boolean vpAnnouncements;

    private boolean securityHeadEnabled;
    private boolean securityHeadWars;

    private boolean customGovernmentEnabled;
    private int maxCustomRoles;
    private boolean playerCustomRoleManagement;

    private boolean leaderModeEnabled;
    private boolean councilModeEnabled;
    private boolean employeeModeEnabled;

    private final Map<NationPermission, Boolean> permissionToggles = new EnumMap<>(NationPermission.class);

    private long councilVoteExpirySeconds;
    private double councilVotePassThreshold;

    public RolesConfig(DirtEconomy plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "roles.yml");
        if (!file.exists()) {
            plugin.saveResource("roles.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        elitesEnabled = config.getBoolean("enabled", true);

        treasurerEnabled = config.getBoolean("treasurer.enabled", true);
        treasurerTreasury = config.getBoolean("treasurer.manage.treasury", true);
        treasurerPropertyApprovals = config.getBoolean("treasurer.manage.property-approvals", true);

        vpEnabled = config.getBoolean("vice-president.enabled", true);
        vpRegions = config.getBoolean("vice-president.manage.regions", true);
        vpWar = config.getBoolean("vice-president.manage.war", true);
        vpTreasuryView = config.getBoolean("vice-president.manage.treasury-view", true);
        vpAnnouncements = config.getBoolean("vice-president.manage.announcements", true);

        securityHeadEnabled = config.getBoolean("security-head.enabled", true);
        securityHeadWars = config.getBoolean("security-head.manage.wars", true);

        customGovernmentEnabled = config.getBoolean("custom-government.enabled", false);
        maxCustomRoles = config.getInt("custom-government.max-custom-roles", 10);
        playerCustomRoleManagement = config.getBoolean("custom-government.player-role-management", false);

        leaderModeEnabled = config.getBoolean("role-modes.leader", true);
        councilModeEnabled = config.getBoolean("role-modes.council", true);
        employeeModeEnabled = config.getBoolean("role-modes.employee", true);

        permissionToggles.clear();
        for (NationPermission perm : NationPermission.values()) {
            boolean enabled = config.getBoolean("custom-permissions." + perm.getConfigKey(), true);
            permissionToggles.put(perm, enabled);
        }

        councilVoteExpirySeconds = config.getLong("council-votes.expiry-seconds", 300);
        councilVotePassThreshold = config.getDouble("council-votes.pass-threshold", 0.5);
    }

    // ──── Legacy Elite getters ────
    public boolean isElitesEnabled() { return elitesEnabled; }

    public boolean isTreasurerEnabled() { return elitesEnabled && treasurerEnabled; }
    public boolean canTreasurerManageTreasury() { return isTreasurerEnabled() && treasurerTreasury; }
    public boolean canTreasurerApproveProperties() { return isTreasurerEnabled() && treasurerPropertyApprovals; }

    public boolean isVicePresidentEnabled() { return elitesEnabled && vpEnabled; }
    public boolean canVpManageRegions() { return isVicePresidentEnabled() && vpRegions; }
    public boolean canVpManageWar() { return isVicePresidentEnabled() && vpWar; }
    public boolean canVpViewTreasury() { return isVicePresidentEnabled() && vpTreasuryView; }
    public boolean canVpManageAnnouncements() { return isVicePresidentEnabled() && vpAnnouncements; }

    public boolean isSecurityHeadEnabled() { return elitesEnabled && securityHeadEnabled; }
    public boolean canSecurityManageWars() { return isSecurityHeadEnabled() && securityHeadWars; }

    // ──── Custom Government getters ────
    public boolean isCustomGovernmentEnabled() { return customGovernmentEnabled; }
    public int getMaxCustomRoles() { return maxCustomRoles; }
    public boolean isPlayerCustomRoleManagement() { return playerCustomRoleManagement; }

    // ──── Role Mode getters ────
    public boolean isLeaderModeEnabled() { return leaderModeEnabled; }
    public boolean isCouncilModeEnabled() { return councilModeEnabled; }
    public boolean isEmployeeModeEnabled() { return employeeModeEnabled; }

    public boolean isRoleModeEnabled(RoleMode mode) {
        return switch (mode) {
            case LEADER -> leaderModeEnabled;
            case COUNCIL -> councilModeEnabled;
            case EMPLOYEE -> employeeModeEnabled;
        };
    }

    // ──── Permission toggle getters ────
    public boolean isPermissionEnabled(NationPermission perm) {
        return permissionToggles.getOrDefault(perm, true);
    }

    public Set<NationPermission> getEnabledPermissions() {
        Set<NationPermission> enabled = EnumSet.noneOf(NationPermission.class);
        for (Map.Entry<NationPermission, Boolean> entry : permissionToggles.entrySet()) {
            if (entry.getValue()) enabled.add(entry.getKey());
        }
        return enabled;
    }

    // ──── Council Vote getters ────
    public long getCouncilVoteExpirySeconds() { return councilVoteExpirySeconds; }
    public double getCouncilVotePassThreshold() { return councilVotePassThreshold; }
}