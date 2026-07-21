package com.dirt.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MailItem {
    private String id;
    private String type; // MARRIAGE_REQUEST, CHILD_BEARING_REQUEST, CHILD_JOIN_REQUEST
    private UUID fromPlayerUuid;
    private long timestamp;
    private Map<String, String> data = new HashMap<>();
}