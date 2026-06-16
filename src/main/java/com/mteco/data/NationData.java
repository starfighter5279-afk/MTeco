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
public class NationData {
    private UUID nationId;
    private String name;
    private String color1 = "§a";
    private String color2 = "§b";
    private UUID presidentUUID;
    private UUID treasurerUUID;
    private UUID vicePresidentUUID;
    private UUID securityHeadUUID;
    private List<UUID> memberUUIDs = new ArrayList<>();
    private List<UUID> regionIds = new ArrayList<>();
    private List<UUID> joinRequestUUIDs = new ArrayList<>();
    private List<UUID> governmentPropertyIds = new ArrayList<>();
    private boolean elitesConfigured = false;
    private double taxRate = 0.0;
    private double lowerClassTaxRate = 0.0;
    private double middleClassTaxRate = 0.0;
    private double upperClassTaxRate = 0.0;

    private List<UUID> enforcerUUIDs = new ArrayList<>();
    private Map<UUID, Double> enforcerSalaries = new HashMap<>();
    private Map<UUID, Integer> enforcerCriminalsCaught = new HashMap<>();
    private double treasuryBalance = 0.0;

    private List<CustomRole> customRoles = new ArrayList<>();
    private boolean customGovernment = false;
}