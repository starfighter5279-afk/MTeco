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
public class WarData {
    private UUID warId;
    private UUID attackingNationId;
    private UUID defendingNationId;
    private List<UUID> attackerRegionIds = new ArrayList<>();
    private List<UUID> targetRegionIds = new ArrayList<>();
}