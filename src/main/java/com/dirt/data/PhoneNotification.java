package com.dirt.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class PhoneNotification {
    private String type;
    private String title;
    private String preview;
    private UUID conversationId;
    private long timestamp;
    private boolean read;
}