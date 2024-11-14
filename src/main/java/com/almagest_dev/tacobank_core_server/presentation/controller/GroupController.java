package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.GroupService;
import com.almagest_dev.tacobank_core_server.domain.friend.model.Friend;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupMemberResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/core/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    // 그룹 생성
    @PostMapping("/{userId}/create")
    public ResponseEntity<GroupResponseDto> createGroup(@PathVariable Long userId, @RequestBody GroupRequestDto requestDto) {
        GroupResponseDto groupResponse = groupService.createGroup(userId, requestDto);
        return ResponseEntity.ok(groupResponse);
    }

    // 그룹 삭제 (그룹장만 가능)
    @DeleteMapping("/{userId}/delete")
    public ResponseEntity<String> deleteGroup(@PathVariable Long userId, @RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        groupService.deleteGroup(userId, groupId);
        return ResponseEntity.ok("그룹이 삭제되었습니다.");
    }

    // 친구 초대 (그룹장만 가능)
    @PostMapping("/{userId}/invite")
    public ResponseEntity<String> inviteFriend(@PathVariable Long userId, @RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        Long friendId = request.get("friendId");
        groupService.inviteFriend(userId, groupId, friendId);
        return ResponseEntity.ok("초대 신청이 완료되었습니다.");
    }

    // 초대 가능한 친구 목록 조회
    @GetMapping("/{userId}/inviteable-friends")
    public ResponseEntity<List<Friend>> getInviteableFriends(@PathVariable Long userId) {
        List<Friend> inviteableFriends = groupService.getInviteableFriends(userId);
        return ResponseEntity.ok(inviteableFriends);
    }

    // 멤버 추방 (그룹장만 가능)
    @PostMapping("/{userId}/expel")
    public ResponseEntity<String> expelMember(@PathVariable Long userId, @RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        Long memberId = request.get("memberId");
        groupService.expelMember(userId, groupId, memberId);
        return ResponseEntity.ok("멤버 추방이 완료되었습니다.");
    }

    // 초대 수락 (멤버만 가능)
    @PostMapping("/{userId}/accept")
    public ResponseEntity<String> acceptInvitation(@PathVariable Long userId, @RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        groupService.acceptInvitation(userId, groupId);
        return ResponseEntity.ok("그룹 승인이 완료되었습니다.");
    }

    // 초대 거절 (멤버만 가능)
    @PostMapping("/{userId}/reject")
    public ResponseEntity<String> rejectInvitation(@PathVariable Long userId, @RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        groupService.rejectInvitation(userId, groupId);
        return ResponseEntity.ok("그룹 요청 거절이 완료되었습니다.");
    }

    // 그룹 나가기 (멤버만 가능)
    @PostMapping("/{userId}/leave")
    public ResponseEntity<String> leaveGroup(@PathVariable Long userId, @RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        groupService.leaveGroup(userId, groupId);
        return ResponseEntity.ok("그룹 나가기가 완료되었습니다.");
    }

    // 내가 속한 그룹 목록 조회
    @GetMapping("/{userId}/my-groups")
    public ResponseEntity<List<GroupResponseDto>> getMyGroups(@PathVariable Long userId) {
        List<GroupResponseDto> myGroups = groupService.getMyGroups(userId);
        return ResponseEntity.ok(myGroups);
    }

    // 초대 대기 목록 조회 (초대를 수락하지 않은 목록)
    @GetMapping("/{userId}/pending-invitations")
    public ResponseEntity<List<GroupMemberResponseDto>> getPendingInvitations(@PathVariable Long userId) {
        List<GroupMemberResponseDto> pendingInvitations = groupService.getPendingInvitations(userId);
        return ResponseEntity.ok(pendingInvitations);
    }
}