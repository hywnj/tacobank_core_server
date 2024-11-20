package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.friend.model.Friend;
import com.almagest_dev.tacobank_core_server.domain.friend.repository.FriendRepository;
import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupMemberRepository;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final FriendRepository friendRepository;
    private final MemberRepository memberRepository;

    public GroupService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
                        FriendRepository friendRepository, MemberRepository memberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.friendRepository = friendRepository;
        this.memberRepository = memberRepository;
    }

    private void validateLeader(Long userId, Group group) {
        if (!group.getLeader().getId().equals(userId)) {
            throw new IllegalStateException("그룹장만 수행할 수 있는 작업입니다.");
        }
    }

    @Transactional
    public GroupResponseDto createGroup(Long userId, GroupRequestDto requestDto) {
        Member leader = memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("그룹장을 찾을 수 없습니다."));

        Group group = new Group();
        group.setLeader(leader);
        group.setActivated("Y");
        group.setCustomized("Y");
        group.setName(requestDto.getGroupName());


        final Group savedGroup = groupRepository.save(group);

        return new GroupResponseDto(
                savedGroup.getId(),
                savedGroup.getName(),
                savedGroup.getCustomized(),
                savedGroup.getActivated(),
                new ArrayList<>() // 멤버 목록은 초기화 상태로 반환
        );
    }

    @Transactional
    public GroupResponseDto createTemporaryGroup(GroupRequestDto requestDto) {
        // 그룹장 찾기
        Member leader = memberRepository.findById(requestDto.getLeaderId())
                .orElseThrow(() -> new IllegalArgumentException("그룹장을 찾을 수 없습니다."));

        // 그룹 생성
        Group group = new Group();
        group.setLeader(leader);
        group.setActivated("N");
        group.setCustomized("N");
        group.setName(leader.getName() + "과 아이들");

        // 저장
        final Group savedGroup = groupRepository.save(group);

        // 친구 목록 추가 (ACCEPTED 상태로)
        if (requestDto.getFriendIds() != null && !requestDto.getFriendIds().isEmpty()) {
            addAcceptedMembersToGroup(savedGroup, requestDto.getFriendIds());
        }

        List<GroupMemberResponseDto> memberDtos = groupMemberRepository.findByPayGroup(savedGroup).stream()
                .map(member -> new GroupMemberResponseDto(
                        member.getId(),
                        savedGroup.getId(),
                        member.getMember().getId(),
                        member.getStatus()
                ))
                .collect(Collectors.toList());

        return new GroupResponseDto(
                savedGroup.getId(),
                savedGroup.getName(),
                savedGroup.getCustomized(),
                savedGroup.getActivated(),
                memberDtos
        );
    }

    private void addAcceptedMembersToGroup(Group group, List<Long> friendIds) {
        for (Long friendId : friendIds) {
            Member friend = memberRepository.findById(friendId)
                    .orElseThrow(() -> new IllegalArgumentException("친구를 찾을 수 없습니다."));

            // GroupMember 생성
            GroupMember groupMember = new GroupMember();
            groupMember.setGroup(group);
            groupMember.setMember(friend);
            groupMember.setStatus("ACCEPTED");

            groupMemberRepository.save(groupMember);
        }
    }

    // 그룹장을 그룹 멤버로 추가하는 메서드
    private void addGroupLeaderAsMember(Group group, Member leader) {
        GroupMember groupLeaderMember = new GroupMember();
        groupLeaderMember.setPayGroup(group);
        groupLeaderMember.setMember(leader);
        groupLeaderMember.setStatus("ACCEPTED");
        groupMemberRepository.save(groupLeaderMember);
    }

    private void addMembersToGroup(Group group, List<Long> friendIds) {
        for (Long friendId : friendIds) {
            Member friend = memberRepository.findById(friendId)
                    .orElseThrow(() -> new IllegalArgumentException("친구를 찾을 수 없습니다."));
            GroupMember groupMember = new GroupMember();
            groupMember.setPayGroup(group);
            groupMember.setMember(friend);
            groupMember.setStatus("INVITED");
            groupMemberRepository.save(groupMember);
        }
    }

    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        validateLeader(userId, group);
        groupRepository.delete(group);
    }

    public void inviteFriend(Long userId, Long groupId, Long friendId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        validateLeader(userId, group);

        // 초대 가능한 친구 ID 목록
        List<Long> inviteableFriendIds = getInviteableFriends(userId).stream()
                .map(friend -> (Long) friend.get("friendId")) // Map에서 "friendId"를 가져옴
                .toList();

        if (!inviteableFriendIds.contains(friendId)) {
            throw new IllegalArgumentException("초대하려는 사용자가 그룹장의 친구가 아닙니다.");
        }

        GroupMember existingMember = groupMemberRepository.findByPayGroupIdAndMemberId(groupId, friendId).orElse(null);

        if (existingMember != null) {
            if ("LEAVED".equals(existingMember.getStatus()) || "EXPELLED".equals(existingMember.getStatus())) {
                existingMember.setStatus("INVITED");
                groupMemberRepository.save(existingMember);
            } else {
                throw new IllegalStateException("이미 그룹에 초대되었거나 수락한 멤버입니다.");
            }
        } else {
            GroupMember groupMember = new GroupMember();
            groupMember.setPayGroup(group);
            groupMember.setMember(memberRepository.findById(friendId)
                    .orElseThrow(() -> new IllegalArgumentException("친구를 찾을 수 없습니다.")));
            groupMember.setStatus("INVITED");
            groupMemberRepository.save(groupMember);
        }
    }

    public List<Map<String, Object>> getInviteableFriends(Long memberId) {
        List<Friend> friends = friendRepository.findByRequesterIdAndStatusOrReceiverIdAndStatus(
                memberId, "ACC", memberId, "ACC");

        // Set을 사용하여 중복 제거
        Set<Map<String, Object>> uniqueFriends = friends.stream()
                .map(friend -> {
                    Long friendId = friend.getRequesterId().equals(memberId)
                            ? friend.getReceiverId()
                            : friend.getRequesterId();
                    String friendName = memberRepository.findById(friendId)
                            .map(Member::getName)
                            .orElse("Unknown");
                    Map<String, Object> friendInfo = new HashMap<>();
                    friendInfo.put("friendId", friendId);
                    friendInfo.put("name", friendName);
                    return friendInfo;
                })
                .collect(Collectors.toSet());

        // Set을 List로 변환
        return new ArrayList<>(uniqueFriends);
    }

    @Transactional
    public void expelMember(Long userId, Long groupId, Long memberId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));
        validateLeader(userId, group);

        GroupMember groupMember = groupMemberRepository.findByPayGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다."));

        if (!"ACCEPTED".equals(groupMember.getStatus())) {
            throw new IllegalStateException("수락한 멤버만 추방할 수 있습니다.");
        }

        groupMember.setStatus("EXPELLED");
        groupMemberRepository.save(groupMember);
    }

    @Transactional
    public void acceptInvitation(Long userId, Long groupId) {
        GroupMember groupMember = groupMemberRepository.findByPayGroupIdAndMemberId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("초대를 찾을 수 없습니다."));

        if ("ACCEPTED".equals(groupMember.getStatus())) {
            throw new IllegalStateException("이미 초대를 수락한 상태입니다.");
        }
        if (!"INVITED".equals(groupMember.getStatus())) {
            throw new IllegalStateException("초대 상태가 아닙니다.");
        }

        groupMember.setStatus("ACCEPTED");
        groupMemberRepository.save(groupMember);
    }

    @Transactional
    public void rejectInvitation(Long userId, Long groupId) {
        GroupMember groupMember = groupMemberRepository.findByPayGroupIdAndMemberId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("초대를 찾을 수 없습니다."));

        if (!"INVITED".equals(groupMember.getStatus())) {
            throw new IllegalStateException("초대 상태가 아닙니다.");
        }

        if ("ACCEPTED".equals(groupMember.getStatus())) {
            throw new IllegalStateException("이미 초대를 수락한 상태에서는 거절할 수 없습니다.");
        }

        groupMember.setStatus("REJECTED");
        groupMemberRepository.save(groupMember);
    }

    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        GroupMember groupMember = groupMemberRepository.findByPayGroupIdAndMemberId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("그룹 멤버를 찾을 수 없습니다."));

        if ("EXPELLED".equals(groupMember.getStatus())) {
            throw new IllegalStateException("추방된 멤버는 그룹을 나갈 수 없습니다.");
        }

        if (!"ACCEPTED".equals(groupMember.getStatus())) {
            throw new IllegalStateException("그룹 나가기가 불가능한 상태입니다.");
        }

        groupMember.setStatus("LEAVED");
        groupMemberRepository.save(groupMember);
    }

    public List<GroupResponseDto> getMyGroups(Long userId) {
        List<Group> myGroups = groupRepository.findByLeaderIdOrMemberId(userId);

        return myGroups.stream()
                .map(group -> new GroupResponseDto(
                        group.getId(),
                        group.getName(),
                        group.getCustomized(),
                        group.getActivated(),
                        group.getPayGroups().stream()
                                .map(member -> new GroupMemberResponseDto(
                                        member.getId(),
                                        group.getId(),
                                        member.getMember().getId(),
                                        member.getStatus()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    public List<GroupMemberResponseDto> getPendingInvitations(Long userId) {
        List<GroupMember> pendingInvitations = groupMemberRepository.findByMemberIdAndStatus(userId, "INVITED");

        return pendingInvitations.stream()
                .map(member -> new GroupMemberResponseDto(
                        member.getId(),
                        member.getPayGroup().getId(),
                        member.getMember().getId(),
                        member.getStatus()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public GroupSearchResponseDto searchGroupByName(String groupName) {
        // 그룹 이름으로 그룹을 조회
        Group group = groupRepository.findByName(groupName)
                .orElseThrow(() -> new IllegalArgumentException("해당 이름의 그룹을 찾을 수 없습니다."));

        // 그룹에 속한 멤버 조회
        List<GropuMemberSearchResponseDto> members = groupMemberRepository.findByPayGroupId(group.getId()).stream()
                .map(groupMember -> new GropuMemberSearchResponseDto(
                        groupMember.getMember().getId(),
                        groupMember.getMember().getName(),
                        groupMember.getStatus()
                ))
                .collect(Collectors.toList());

        // 그룹 정보와 멤버 정보를 DTO로 반환
        return new GroupSearchResponseDto(
                group.getId(),
                group.getName(),
                members
        );
    }
}