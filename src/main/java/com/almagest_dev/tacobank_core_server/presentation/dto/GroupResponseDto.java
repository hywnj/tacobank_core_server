package com.almagest_dev.tacobank_core_server.presentation.dto;

public class GroupResponseDto {
    private Long groupId;
    private String groupName;
    private String customized;
    private String activated;

    public GroupResponseDto(Long groupId, String groupName, String customized, String activated) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.customized = customized;
        this.activated = activated;
    }
}
