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
public class CharacterData {
    private UUID playerUuid;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender; // "MALE" or "FEMALE"
    private long birthDate;
    private boolean alive = true;
    private UUID familyId;
    private UUID birthFamilyId;
    private String familyRole; // "PRIMARY", "SECONDARY", or "CHILD"
    private long firstJoinDate;
    private UUID inheritorUuid;
    private boolean pendingChildSelection = false;
    private List<UUID> previousFamilyIds = new ArrayList<>();
    private List<MailItem> mailbox = new ArrayList<>();
    private long lastDailyReward = 0;
    private String discordId;
    private String homeLocation;
}