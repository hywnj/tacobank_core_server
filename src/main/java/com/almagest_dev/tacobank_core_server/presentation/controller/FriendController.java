package com.almagest_dev.tacobank_core_server.presentation.controller;


import com.almagest_dev.tacobank_core_server.presentation.dto.FriendRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.FriendResponseDto;
import com.almagest_dev.tacobank_core_server.application.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/{userId}/request")
    public ResponseEntity<String> requestFriend(@PathVariable Long userId, @RequestBody FriendRequestDto requestDto) {
        friendService.requestFriend(userId, requestDto);
        return ResponseEntity.ok("친구 요청이 성공적으로 처리되었습니다.");
    }

    @PostMapping("/{userId}/accept")
    public ResponseEntity<String> acceptFriend(@PathVariable Long userId, @RequestBody FriendRequestDto requestDto) {
        friendService.acceptFriend(userId, requestDto);
        return ResponseEntity.ok("친구 요청을 수락하였습니다.");
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<String> rejectFriend(@PathVariable Long userId, @RequestBody FriendRequestDto requestDto) {
        friendService.rejectFriend(userId, requestDto);
        return ResponseEntity.ok("친구 요청을 거절하였습니다.");
    }

    @PostMapping("/{userId}/block")
    public ResponseEntity<String> blockFriend(@PathVariable Long userId, @RequestBody FriendRequestDto requestDto) {
        friendService.blockFriend(userId, requestDto);
        return ResponseEntity.ok("친구를 차단하였습니다.");
    }

    @PostMapping("/{userId}/unblock")
    public ResponseEntity<String> unblockFriend(@PathVariable Long userId, @RequestBody FriendRequestDto requestDto) {
        friendService.unblockFriend(userId, requestDto);
        return ResponseEntity.ok("친구 차단을 해제하였습니다.");
    }

    @PostMapping("/{userId}/delete")
    public ResponseEntity<String> deleteFriend(@PathVariable Long userId, @RequestBody FriendRequestDto requestDto) {
        friendService.deleteFriend(userId, requestDto);
        return ResponseEntity.ok("친구를 삭제하였습니다.");
    }

    @GetMapping("/{userId}/blocked")
    public ResponseEntity<List<FriendResponseDto>> getBlockedFriends(@PathVariable Long userId) {
        List<FriendResponseDto> blockedFriends = friendService.getBlockedFriends(userId);
        return ResponseEntity.ok(blockedFriends);
    }

    @GetMapping("/{userId}/list")
    public ResponseEntity<List<FriendResponseDto>> getFriendList(@PathVariable Long userId) {
        List<FriendResponseDto> friends = friendService.getFriendList(userId);
        return ResponseEntity.ok(friends);
    }

    @PostMapping("/{userId}/like")
    public ResponseEntity<String> likeFriend(@PathVariable Long userId, @RequestBody FriendRequestDto requestDto) {
        friendService.likeFriend(userId, requestDto);
        return ResponseEntity.ok("친구에게 좋아요를 눌렀습니다.");
    }

    @PostMapping("/{userId}/unlike")
    public ResponseEntity<String> unlikeFriend(@PathVariable Long userId, @RequestBody FriendRequestDto requestDto) {
        friendService.unlikeFriend(userId, requestDto);
        return ResponseEntity.ok("친구에게 좋아요 취소를 했습니다.");
    }
}
