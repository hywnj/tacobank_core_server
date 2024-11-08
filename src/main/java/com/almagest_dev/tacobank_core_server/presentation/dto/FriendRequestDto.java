package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendRequestDto {
    private String requesterId;
    private String receiverId;
}