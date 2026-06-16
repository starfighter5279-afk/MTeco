package com.mteco.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class CriminalRecord {
    private UUID recordId;
    private UUID nationId;
    private UUID criminalUUID;
    private List<UUID> crimeIds = new ArrayList<>();
    private String description;
    private long convictedTimestamp;
    private long jailReleaseTime;
    private boolean serving = false;
    private UUID cellId;
    private boolean escaped = false;
    private String assignedBedLocation;
}