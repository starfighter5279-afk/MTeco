package com.dirt.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class StockroomData {
    private UUID stockroomId;
    private UUID businessId;
    private String name;
    private String world;
    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    public boolean contains(String w, int x, int y, int z) {
        return world.equals(w) && x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }
}