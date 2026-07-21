package com.dirt.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PhoneData {
    private UUID ownerUuid;
    private String username;
    private List<PhoneContact> contacts = new ArrayList<>();
    private List<UUID> conversationIds = new ArrayList<>();
    private List<PhoneNotification> notifications = new ArrayList<>();
    private Set<String> installedApps = new HashSet<>();
    private List<UUID> pinnedGpsLocations = new ArrayList<>();
}