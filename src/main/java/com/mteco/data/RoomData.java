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
public class RoomData {
    private UUID roomId;
    private UUID propertyId;
    private String name;
    private String world;
    private int corner1X, corner1Y, corner1Z;
    private int corner2X, corner2Y, corner2Z;
    private int ceilingY;
    private boolean showTitle = true;

    private Map<String, Boolean> permsSameRegion = defaultPerms(true, true, false, false);
    private Map<String, Boolean> permsSameNation = defaultPerms(true, false, false, false);
    private Map<String, Boolean> permsForeign = defaultPerms(false, false, false, false);

    private Map<UUID, Map<String, Boolean>> characterPerms = new HashMap<>();
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