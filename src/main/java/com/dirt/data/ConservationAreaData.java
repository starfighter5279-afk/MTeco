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
public class ConservationAreaData {
    private UUID areaId;
    private UUID regionId;
    private String name;
    private List<String> chunks = new ArrayList<>();
}