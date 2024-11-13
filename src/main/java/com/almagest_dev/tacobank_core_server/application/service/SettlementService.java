package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupMemberRepository;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupRepository;
import com.almagest_dev.tacobank_core_server.domain.settlememt.model.Settlement;
import com.almagest_dev.tacobank_core_server.presentation.dto.SettlementRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.SettlementResponseDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SettlementService {
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    public SettlementService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
    }

    public List<SettlementResponseDto> calculateSettlement(SettlementRequestDto request) {
        // 그룹 정보 가져오기
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));

        // ACCEPTED 상태의 그룹 멤버 목록 가져오기
        List<GroupMember> acceptedMembers = groupMemberRepository.findByPayGroupAndStatus(group, "ACCEPTED");

        // 그룹장도 정산에 포함
        int totalMembers = acceptedMembers.size() + 1; // 그룹장 포함한 멤버 수

        int perMemberAmount = request.getTotalAmount() / totalMembers;

        // 각 ACCEPTED 멤버의 분담 금액 정보 생성
        List<SettlementResponseDto> settlementList = new ArrayList<>(acceptedMembers.stream()
                .map(member -> new SettlementResponseDto(member.getMember().getId(), perMemberAmount))
                .toList());

        // 그룹장 분담 금액 정보 추가
        settlementList.add(new SettlementResponseDto(group.getLeader().getId(), perMemberAmount));

        return settlementList;
    }
}