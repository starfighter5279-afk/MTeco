package com.dirt.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CellData {
    private UUID cellId;
    private UUID jailId;
    private String name;
    private String location;
    private int corner1X, corner1Y, corner1Z;
    private int corner2X, corner2Y, corner2Z;
    private int cellLimit = 2;
}