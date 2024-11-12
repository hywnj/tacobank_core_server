package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.GroupService;
import com.almagest_dev.tacobank_core_server.domain.friend.model.Friend;
import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
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
    @PostMapping("/create")
    public ResponseEntity<GroupResponseDto> createGroup(@RequestBody GroupRequestDto requestDto) {
        GroupResponseDto groupResponse = groupService.createGroup(requestDto);
        return ResponseEntity.ok(groupResponse);
    }

    // 그룹 삭제 (그룹장만 가능)
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteGroup(@RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        groupService.deleteGroup(groupId);
        return ResponseEntity.ok("그룹이 삭제 되었습니다.");
    }

    // 친구 초대 (그룹장만 가능)
    @PostMapping("/invite")
    public ResponseEntity<String> inviteFriend(@RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        Long friendId = request.get("friendId");
        groupService.inviteFriend(groupId, friendId);
        return ResponseEntity.ok("초대 신청이 되었습니다.");
    }

    // 초대 가능한 친구 목록 조회
    @PostMapping("/inviteable-friends")
    public ResponseEntity<List<Friend>> getInviteableFriends(@RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        Long leaderId = groupService.getCurrentUserId(); // 임시로 그룹장을 현재 사용자로 설정
        List<Friend> inviteableFriends = groupService.getInviteableFriends(leaderId);
        return ResponseEntity.ok(inviteableFriends);
    }

    // 멤버 추방 (그룹장만 가능)
    @PostMapping("/expel")
    public ResponseEntity<String> expelMember(@RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        Long memberId = request.get("memberId");
        groupService.expelMember(groupId, memberId);
        return ResponseEntity.ok("멤버 추방이 완료되었습니다.");
    }

    // 초대 수락 (멤버만 가능)
    @PostMapping("/accept")
    public ResponseEntity<String > acceptInvitation(@RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        groupService.acceptInvitation(groupId);
        return ResponseEntity.ok("그룹 승인이 완료되었습니다.");
    }

    // 초대 거절 (멤버만 가능)
    @PostMapping("/reject")
    public ResponseEntity<String> rejectInvitation(@RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        groupService.rejectInvitation(groupId);
        return ResponseEntity.ok("그룹 요청 거절이 완료되었습니다.");
    }

    // 그룹 나가기 (멤버만 가능)
    @PostMapping("/leave")
    public ResponseEntity<String> leaveGroup(@RequestBody Map<String, Long> request) {
        Long groupId = request.get("groupId");
        groupService.leaveGroup(groupId);
        return ResponseEntity.ok("그룹 나가기가 완료되었습니다.");
    }

    // 내가 속한 그룹 목록 조회
    @GetMapping("/my-groups")
    public ResponseEntity<List<GroupResponseDto>> getMyGroups() {
        List<GroupResponseDto> myGroups = groupService.getMyGroups();
        return ResponseEntity.ok(myGroups);
    }

    // 초대 대기 목록 조회 (초대를 수락하지 않은 목록)
    @GetMapping("/pending-invitations")
    public ResponseEntity<List<GroupMemberResponseDto>> getPendingInvitations() {
        List<GroupMemberResponseDto> pendingInvitations = groupService.getPendingInvitations();
        return ResponseEntity.ok(pendingInvitations);
    }
}