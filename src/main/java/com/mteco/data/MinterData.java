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
public class MinterData {
    private UUID minterId;
    private UUID nationId;
    private String world;
    private int baseX, baseY, baseZ;
    private String signFace;
    private List<String> queue = new ArrayList<>();
    private long processingStartTime = 0;
}