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
public class LawBookData {
    private UUID bookId;
    private UUID nationId;
    private UUID authorUUID;
    private String title;
    private List<String> sections = new ArrayList<>();
    private boolean approved = true;
    private boolean pendingApproval = false;
}