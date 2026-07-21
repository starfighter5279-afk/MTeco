package com.dirt.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class FastTravelPoint {
    private UUID pointId;
    private String world;
    private int x;
    private int y;
    private int z;
    private UUID creatorUuid;
    private long createdAt;
    private long visits;
}