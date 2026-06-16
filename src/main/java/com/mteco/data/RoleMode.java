package com.mteco.data;

public enum RoleMode {
    LEADER("Leader", "Supreme leader with all permissions. Actions require council approval."),
    COUNCIL("Council", "Votes on leader actions. If no leader, votes on each other's actions."),
    EMPLOYEE("Employee", "Standard role with assigned permissions.");

    private final String displayName;
    private final String description;

    RoleMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}