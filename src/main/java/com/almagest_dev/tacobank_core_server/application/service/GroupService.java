package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.friend.model.Friend;
import com.almagest_dev.tacobank_core_server.domain.friend.repository.FriendRepository;
import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupMemberRepository;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.GroupResponseDto;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

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

    @Transactional
    public GroupResponseDto createGroup(GroupRequestDto requestDto) {
        Long leaderId = requestDto.getLeaderId();

        if ("N".equals(requestDto.getCustomized())) {
            return new GroupResponseDto(null, "Temporary Group", "N", "Y");
        }

        Member leader = memberRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("그룹장을 찾을 수 없습니다."));

        // 커스텀 그룹 생성 로직
        Group group = new Group();
        group.setName(requestDto.getGroupName());
        group.setLeader(leader);
        group.setActivated("Y");
        group.setCustomized("Y");
        group = groupRepository.save(group);

        // 그룹 멤버 추가 로직
        addMembersToGroup(group, requestDto.getFriendIds());

        return new GroupResponseDto(group.getId(), group.getName(), group.getCustomized(), group.getActivated());
    }

    // 그룹에 멤버 추가
    private void addMembersToGroup(Group group, List<Long> friendIds) {
        for (Long friendId : friendIds) {
            Member friend = memberRepository.findById(friendId)
                    .orElseThrow(() -> new IllegalArgumentException("친구를 찾을 수 없습니다."));

            GroupMember groupMember = new GroupMember();
            groupMember.setPayGroup(group);
            groupMember.setMember(friend);
            groupMember.setStatus("INVITED"); // 초대 상태로 설정
            groupMemberRepository.save(groupMember);
        }
    }

    // 초대 가능한 친구 목록 조회
    public List<Friend> getInviteableFriends(Long leaderId) {
        return friendRepository.findByRequesterIdAndStatusOrReceiverIdAndStatus(
                leaderId, "ACC", leaderId, "ACC");
    }

}