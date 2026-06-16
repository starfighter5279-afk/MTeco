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
public class FamilyData {
    private UUID familyId;
    private UUID spouse1;
    private UUID spouse2;
    private List<UUID> children = new ArrayList<>();
}