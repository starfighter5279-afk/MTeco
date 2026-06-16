package com.mteco.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PropertyData {
    private UUID propertyId;
    private UUID regionId;
    private UUID ownerUUID;
    private List<String> chunks = new ArrayList<>();
    private String name;
    private boolean forSale = false;
    private double salePrice = 0.0;
    private String saleSignLocation;

    private Map<UUID, Double> offers = new HashMap<>();
    private Map<String, Boolean> permsSameRegion = defaultPerms(true, true, false, false);
    private Map<String, Boolean> permsSameNation = defaultPerms(true, false, false, false);
    private Map<String, Boolean> permsForeign = defaultPerms(false, false, false, false);
    private Set<String> deniedMobs = new HashSet<>();

    private List<UUID> roomIds = new ArrayList<>();
    private Map<UUID, Map<String, Boolean>> rolePerms = new HashMap<>();

    private static Map<String, Boolean> defaultPerms(boolean enter, boolean interact, boolean build, boolean containers) {
        Map<String, Boolean> map = new HashMap<>();
        map.put("enter", enter);
        map.put("interact", interact);
        map.put("build", build);
        map.put("containers", containers);
        return map;
    }
}