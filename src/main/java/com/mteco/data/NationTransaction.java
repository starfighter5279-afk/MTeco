package com.mteco.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NationTransaction {
    private String type;
    private double amount;
    private String description;
    private long timestamp;
}