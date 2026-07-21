package com.dirt.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Location;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class TravelDestination {
    private final String name;
    private final String description;
    private final Location location;
    private final UUID fastTravelPointId;
}