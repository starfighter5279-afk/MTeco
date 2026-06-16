package com.mteco.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class BusinessData {
    private UUID businessId;
    private String name;
    private String description;
    private UUID ownerUUID;
    private double payrollRate = 0.0;
    private Map<UUID, Double> employeeRates = new HashMap<>();
    private List<UUID> employeeUUIDs = new ArrayList<>();
    private double totalEarned = 0.0;
    private double treasuryBalance = 0.0;
    private boolean hiring = false;
    private boolean forSale = false;
    private double salePrice = 0.0;
    private List<UUID> propertyIds = new ArrayList<>();
    private List<BusinessRole> roles = new ArrayList<>();
    private UUID defaultRoleId;

    public boolean hasPermission(UUID playerUuid, BusinessPermission perm) {
        if (playerUuid.equals(ownerUUID)) return true;
        for (BusinessRole role : roles) {
            if (role.getMemberUUIDs().contains(playerUuid) && role.getPermissions().contains(perm)) {
                return true;
            }
        }
        return false;
    }

    public BusinessRole getRoleForEmployee(UUID playerUuid) {
        for (BusinessRole role : roles) {
            if (role.getMemberUUIDs().contains(playerUuid)) return role;
        }
        return null;
    }

    public BusinessRole getRoleById(UUID roleId) {
        for (BusinessRole role : roles) {
            if (role.getRoleId().equals(roleId)) return role;
        }
        return null;
    }

    public BusinessRole getDefaultRole() {
        if (defaultRoleId == null) return null;
        return getRoleById(defaultRoleId);
    }
}