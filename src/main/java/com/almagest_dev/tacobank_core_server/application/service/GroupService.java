package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.friend.model.Friend;
import com.almagest_dev.tacobank_core_server.domain.friend.repository.FriendRepository;
import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupMemberRepository;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupMemberResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupResponseDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public Long getCurrentUserId() {
        // String currentUserId = SecurityContextHolder.getContext().getAuthentication().getName();
        return 1L; // 테스트용 임시 사용자 ID
    }

    @Transactional
    public GroupResponseDto createGroup(GroupRequestDto requestDto) {
        Long leaderId = getCurrentUserId();
        Member leader = memberRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("그룹장을 찾을 수 없습니다."));

        Group group = new Group();
        group.setLeader(leader);
        group.setActivated("Y");
        group.setCustomized(requestDto.getCustomized());

        if ("N".equals(requestDto.getCustomized())) {
            group.setName("Temporary Group");
        } else {
            group.setName(requestDto.getGroupName());
        }

        final Group savedGroup = groupRepository.save(group); // group을 final로 선언하여 저장

        // 그룹 멤버 추가 로직
        addMembersToGroup(savedGroup, requestDto.getFriendIds());

        // GroupMemberResponseDto 리스트 생성
        List<GroupMemberResponseDto> memberDtos = savedGroup.getPayGroups().stream()
                .map(member -> new GroupMemberResponseDto(
                        member.getId(),
                        savedGroup.getId(),
                        member.getMember().getId(),
                        member.getStatus()
                ))
                .collect(Collectors.toList());

        return new GroupResponseDto(savedGroup.getId(), savedGroup.getName(), savedGroup.getCustomized(), savedGroup.getActivated(), memberDtos);
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
    public void deleteGroup(Long groupId) {
        Long leaderId = getCurrentUserId();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        if (!group.getLeader().getId().equals(leaderId)) {
            throw new IllegalStateException("그룹장만 그룹을 삭제할 수 있습니다.");
        }

        groupRepository.delete(group);
    }

    @Transactional
    public void inviteFriend(Long groupId, Long friendId) {
        Long leaderId = getCurrentUserId();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        if (!group.getLeader().getId().equals(leaderId)) {
            throw new IllegalStateException("그룹장만 초대할 수 있습니다.");
        }

        List<Long> inviteableFriendIds = getInviteableFriends(leaderId).stream()
                .map(friend -> friend.getRequesterId().equals(leaderId) ? friend.getReceiverId() : friend.getRequesterId())
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

    public List<Friend> getInviteableFriends(Long leaderId) {
        return friendRepository.findByRequesterIdAndStatusOrReceiverIdAndStatus(
                leaderId, "ACC", leaderId, "ACC");
    }

    @Transactional
    public void expelMember(Long groupId, Long memberId) {
        Long leaderId = getCurrentUserId();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("그룹을 찾을 수 없습니다."));

        if (!group.getLeader().getId().equals(leaderId)) {
            throw new IllegalStateException("그룹장만 멤버를 추방할 수 있습니다.");
        }

        GroupMember groupMember = groupMemberRepository.findByPayGroupIdAndMemberId(groupId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다."));

        if (!"ACCEPTED".equals(groupMember.getStatus())) {
            throw new IllegalStateException("수락한 멤버만 추방할 수 있습니다.");
        }

        groupMember.setStatus("EXPELLED");
        groupMemberRepository.save(groupMember);
    }

    @Transactional
    public void acceptInvitation(Long groupId) {
        Long memberId = getCurrentUserId();
        GroupMember groupMember = groupMemberRepository.findByPayGroupIdAndMemberId(groupId, memberId)
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
    public void rejectInvitation(Long groupId) {
        Long memberId = getCurrentUserId();
        GroupMember groupMember = groupMemberRepository.findByPayGroupIdAndMemberId(groupId, memberId)
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
    public void leaveGroup(Long groupId) {
        Long memberId = getCurrentUserId();
        GroupMember groupMember = groupMemberRepository.findByPayGroupIdAndMemberId(groupId, memberId)
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

    public List<GroupResponseDto> getMyGroups() {
        Long memberId = getCurrentUserId();
        List<Group> myGroups = groupRepository.findByLeaderIdOrMemberId(memberId);

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

    public List<GroupMemberResponseDto> getPendingInvitations() {
        Long memberId = getCurrentUserId();
        List<GroupMember> pendingInvitations = groupMemberRepository.findByMemberIdAndStatus(memberId, "INVITED");

        return pendingInvitations.stream()
                .map(member -> new GroupMemberResponseDto(
                        member.getId(),
                        member.getPayGroup().getId(),
                        member.getMember().getId(),
                        member.getStatus()
                ))
                .collect(Collectors.toList());
    }
}