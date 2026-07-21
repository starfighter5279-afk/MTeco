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
public class PhoneConversation {
    private UUID conversationId;
    private String name;
    private List<UUID> memberUuids = new ArrayList<>();
    private List<PhoneMessage> messages = new ArrayList<>();
    private boolean groupChat;
}