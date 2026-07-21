package com.dirt.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GPSLocation {
    private UUID locationId;
    private String name;
    private String description;
    private UUID creatorUuid;
    private UUID businessId;
    private String world;
    private double x;
    private double y;
    private double z;
    private boolean global;
    private long createdAt;
}