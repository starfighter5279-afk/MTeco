package com.dirt.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
public class BusinessPropertyData {
    private UUID propertyId;
    private UUID businessId;
    private String name;
    private List<String> chunks = new ArrayList<>();

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