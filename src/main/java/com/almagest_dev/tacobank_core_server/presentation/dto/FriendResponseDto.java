package com.almagest_dev.tacobank_core_server.presentation.dto;

import com.almagest_dev.tacobank_core_server.domain.friend.model.Friend;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class FriendResponseDto {
    private Long friendId;
    private String friendName;
    private String liked;
}
