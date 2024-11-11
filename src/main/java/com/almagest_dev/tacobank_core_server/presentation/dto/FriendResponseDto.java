package com.almagest_dev.tacobank_core_server.presentation.dto;

import com.almagest_dev.tacobank_core_server.domain.friend.model.Friend;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class FriendResponseDto {
    private Long id;
    private Long requesterId;
    private Long receiverId;
    private String status;
    private String liked;

    public FriendResponseDto(Friend friend) {
        this.id = friend.getId();
        this.requesterId = friend.getRequesterId();
        this.receiverId = friend.getReceiverId();
        this.status = friend.getStatus();
        this.liked = friend.getLiked();
    }

}
