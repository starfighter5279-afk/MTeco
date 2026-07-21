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
public class PropertyPurchaseRequest {
    private UUID requestId;
    private UUID regionId;
    private UUID requesterUUID;
    private List<String> requestedChunks = new ArrayList<>();
    private double totalPrice;
    private boolean governorApproved = false;
    private boolean treasurerApproved = false;
    private String propertyName = "";
}