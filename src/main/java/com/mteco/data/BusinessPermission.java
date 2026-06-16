package com.mteco.data;

import com.cryptomorin.xseries.XMaterial;

public enum BusinessPermission {
    MANAGE_EMPLOYEES("Employee Management", "Hire, fire, and manage employees", XMaterial.PLAYER_HEAD),
    MANAGE_ROLES("Role Management", "Create and manage business roles", XMaterial.NAME_TAG),
    MANAGE_TREASURY("Treasury Management", "Deposit and withdraw from treasury", XMaterial.GOLD_INGOT),
    MANAGE_PAYROLL("Payroll Management", "Set employee pay rates", XMaterial.GOLD_NUGGET),
    MANAGE_PROPERTY("Property Management", "Manage business properties", XMaterial.OAK_DOOR),
    MANAGE_SHOPS("Shop Management", "Manage business shops", XMaterial.CHEST),
    MANAGE_CONTRACTS("Contract Management", "Create, sign, and manage contracts", XMaterial.WRITABLE_BOOK),
    MANAGE_SETTINGS("Settings Management", "Change business name, description, hiring", XMaterial.COMPARATOR),
    MANAGE_SALES("Sales Management", "List business for sale", XMaterial.EMERALD);

    private final String displayName;
    private final String description;
    private final XMaterial icon;

    BusinessPermission(String displayName, String description, XMaterial icon) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public XMaterial getIcon() { return icon; }

    public String getConfigKey() {
        return name().toLowerCase().replace('_', '-');
    }
}