package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.presentation.dto.friend.FriendRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.friend.FriendResponseDto;
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

    @PostMapping("/request")
    public ResponseEntity<String> requestFriend(@RequestBody FriendRequestDto requestDto) {
        friendService.requestFriend(requestDto.getRequesterId(), requestDto);
        return ResponseEntity.ok("친구 요청이 성공적으로 처리되었습니다.");
    }

    @PostMapping("/accept")
    public ResponseEntity<String> acceptFriend(@RequestBody FriendRequestDto requestDto) {
        friendService.acceptFriend(requestDto.getRequesterId(), requestDto);
        return ResponseEntity.ok("친구 요청을 수락하였습니다.");
    }

    @PostMapping("/reject")
    public ResponseEntity<String> rejectFriend(@RequestBody FriendRequestDto requestDto) {
        friendService.rejectFriend(requestDto.getRequesterId(), requestDto);
        return ResponseEntity.ok("친구 요청을 거절하였습니다.");
    }

    @PostMapping("/block")
    public ResponseEntity<String> blockFriend(@RequestBody FriendRequestDto requestDto) {
        friendService.blockFriend(requestDto.getRequesterId(), requestDto);
        return ResponseEntity.ok("친구를 차단하였습니다.");
    }

    @PostMapping("/unblock")
    public ResponseEntity<String> unblockFriend(@RequestBody FriendRequestDto requestDto) {
        friendService.unblockFriend(requestDto.getRequesterId(), requestDto);
        return ResponseEntity.ok("친구 차단을 해제하였습니다.");
    }

    @PostMapping
    public ResponseEntity<String> deleteFriend(@RequestBody FriendRequestDto requestDto) {
        friendService.deleteFriend(requestDto.getRequesterId(), requestDto);
        return ResponseEntity.ok("친구를 삭제하였습니다.");
    }

    @GetMapping("/blocked")
    public ResponseEntity<List<FriendResponseDto>> getBlockedFriends(@RequestBody FriendRequestDto requestDto) {
        List<FriendResponseDto> blockedFriends = friendService.getBlockedFriends(requestDto.getRequesterId());
        return ResponseEntity.ok(blockedFriends);
    }


    @GetMapping("/list")
    public ResponseEntity<List<FriendResponseDto>> getFriendList(@RequestBody FriendRequestDto requestDto) {
        Long requesterId = requestDto.getRequesterId(); // 요청 본문에서 requesterId 추출
        List<FriendResponseDto> friends = friendService.getFriendList(requesterId);
        return ResponseEntity.ok(friends);
    }

    @PostMapping("/like")
    public ResponseEntity<String> likeFriend(@RequestBody FriendRequestDto requestDto) {
        friendService.likeFriend(requestDto.getRequesterId(), requestDto);
        return ResponseEntity.ok("친구에게 좋아요를 눌렀습니다.");
    }

    @PostMapping("/unlike")
    public ResponseEntity<String> unlikeFriend(@RequestBody FriendRequestDto requestDto) {
        friendService.unlikeFriend(requestDto.getRequesterId(), requestDto);
        return ResponseEntity.ok("친구에게 좋아요 취소를 했습니다.");
    }

    @GetMapping("/received/list")
    public ResponseEntity<List<FriendResponseDto>> getReceivedFriendRequestsByBody(@RequestBody FriendRequestDto requestDto) {
        Long requesterId = requestDto.getRequesterId();// JSON 본문에서 requestId 추출
        List<FriendResponseDto> receivedRequests = friendService.getReceivedFriendRequests(requesterId);
        return ResponseEntity.ok(receivedRequests);
    }
}