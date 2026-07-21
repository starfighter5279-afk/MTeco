package com.dirt.data;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public class CouncilVote {
    private UUID voteId;
    private UUID nationId;
    private UUID initiatorUUID;
    private NationPermission permission;
    private String description;
    private Map<UUID, Boolean> votes = new HashMap<>();
    private List<UUID> eligibleVoters = new ArrayList<>();
    private long createdTimestamp;
    private long expiryTimestamp;
    private transient Runnable onPass;
    private transient Runnable onDeny;
}