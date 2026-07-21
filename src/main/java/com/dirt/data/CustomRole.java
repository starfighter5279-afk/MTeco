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
public class CustomRole {
    private UUID roleId;
    private String name;
    private String colorCode = "\u00a7f";
    private RoleMode roleMode = RoleMode.EMPLOYEE;
    private Set<NationPermission> permissions = new HashSet<>();
    private List<UUID> memberUUIDs = new ArrayList<>();
}