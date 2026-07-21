package com.dirt.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PhoneMessage {
    private UUID senderUuid;
    private String senderUsername;
    private String content;
    private long timestamp;
}