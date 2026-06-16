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
public class LegislatedCrimeData {
    private UUID crimeId;
    private UUID nationId;
    private String name;
    private int minJailDays = 1;
    private double fineAmount = 0.0;
    private List<String> linkedLawSections = new ArrayList<>();
    private boolean approved = true;
    private boolean pendingApproval = false;
}