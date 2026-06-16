package com.mteco.data;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ContractSignature {
    private UUID playerUuid;
    private String characterName;
    private String characterCode;
    private String onBehalfOf;
    private UUID onBehalfOfId;
    private String onBehalfOfType;
    private long signedAt;
}