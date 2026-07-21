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
public class ContractData {
    private UUID contractId;
    private String body = "";
    private String title = "";
    private UUID creatorPlayerUuid;
    private String creatorCharacterName;
    private long createdAt;
    private int durationMinecraftDays;
    private long expiresAt;
    private String status = "DRAFT";
    private List<ContractSignature> signatures = new ArrayList<>();
    private UUID pendingRecipientUuid;
    private UUID secondRecipientUuid;
    private List<UUID> linkedPlayerUuids = new ArrayList<>();
    private List<UUID> linkedBusinessIds = new ArrayList<>();
    private List<UUID> linkedNationIds = new ArrayList<>();
}