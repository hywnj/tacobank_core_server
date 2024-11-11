package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.GroupService;
import com.almagest_dev.tacobank_core_server.domain.friend.model.Friend;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    private Long getCurrentUserId() {
        // String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return 1L;
    }

    @PostMapping("/create")
    public ResponseEntity<GroupResponseDto> createGroup(@RequestBody GroupRequestDto requestDto) {
        GroupResponseDto groupResponse = groupService.createGroup(requestDto);
        return ResponseEntity.ok(groupResponse);
    }

    // 초대 가능한 친구 목록 조회 엔드포인트
    @GetMapping("/{leaderId}/inviteable-friends")
    public ResponseEntity<List<Friend>> getInviteableFriends(@PathVariable Long leaderId) {
        List<Friend> friends = groupService.getInviteableFriends(leaderId);
        return ResponseEntity.ok(friends);
    }


}