package com.dirt.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class JailData {
    private UUID jailId;
    private UUID nationId;
    private String name;
    private String world;
    private int corner1X, corner1Y, corner1Z;
    private int corner2X, corner2Y, corner2Z;
    private List<UUID> cellIds = new ArrayList<>();
    private List<String> cellLocations = new ArrayList<>();
}