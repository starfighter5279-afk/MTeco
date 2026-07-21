package com.dirt.data;

import com.cryptomorin.xseries.XMaterial;

public enum NationPermission {
    // ──── Government ────
    MANAGE_MEMBERS("Manage Members", "Invite, kick, and manage nation members", XMaterial.PLAYER_HEAD, "Government"),
    INVITE_MEMBERS("Invite Members", "Send invitations to join the nation", XMaterial.OAK_SIGN, "Government"),
    KICK_MEMBERS("Kick Members", "Remove members from the nation", XMaterial.IRON_AXE, "Government"),
    MANAGE_ROLES("Manage Roles", "Create and manage custom nation roles", XMaterial.NAME_TAG, "Government"),
    MANAGE_ANNOUNCEMENTS("Announcements", "Broadcast messages to nation members", XMaterial.BELL, "Government"),

    // ──── Territory ────
    MANAGE_REGIONS("Manage Regions", "Create and manage nation regions", XMaterial.MAP, "Territory"),
    MANAGE_PROPERTIES("Manage Properties", "Government properties and approvals", XMaterial.DARK_OAK_DOOR, "Territory"),
    MANAGE_CONSERVATION("Conservation Areas", "Create and manage conservation areas", XMaterial.OAK_SAPLING, "Territory"),

    // ──── Economy ────
    MANAGE_TREASURY("Manage Treasury", "Withdraw and manage nation treasury funds", XMaterial.GOLD_INGOT, "Economy"),
    VIEW_TREASURY("View Treasury", "View treasury balance and transaction history", XMaterial.PAPER, "Economy"),
    MANAGE_TAX("Manage Tax", "Set and manage nation tax rates", XMaterial.GOLD_NUGGET, "Economy"),
    MANAGE_MINTERS("Manage Minters", "Create and delete nation minters", XMaterial.FURNACE, "Economy"),
    USE_MINTERS("Use Minters", "Deposit gold into minters for processing", XMaterial.CAULDRON, "Economy"),

    // ──── Security ────
    MANAGE_ENFORCERS("Manage Enforcers", "Hire, fire, and manage enforcers", XMaterial.IRON_CHESTPLATE, "Security"),
    MANAGE_LAWS("Manage Laws", "Create, approve, and manage law books", XMaterial.BOOK, "Security"),
    MANAGE_CRIME("Manage Crime", "Convict criminals and manage records", XMaterial.IRON_BARS, "Security"),
    MANAGE_JAILS("Manage Jails", "Create and manage jails and cells", XMaterial.IRON_DOOR, "Security"),
    MANAGE_WAR("Manage War", "Declare and manage wars with other nations", XMaterial.DIAMOND_SWORD, "Security"),

    // ──── Diplomacy ────
    MANAGE_CONTRACTS("Contract Authority", "Sign contracts on behalf of the nation", XMaterial.WRITABLE_BOOK, "Diplomacy");

    private final String displayName;
    private final String description;
    private final XMaterial icon;
    private final String category;

    NationPermission(String displayName, String description, XMaterial icon, String category) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.category = category;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public XMaterial getIcon() { return icon; }
    public String getCategory() { return category; }

    public String getConfigKey() {
        return name().toLowerCase().replace('_', '-');
    }
}