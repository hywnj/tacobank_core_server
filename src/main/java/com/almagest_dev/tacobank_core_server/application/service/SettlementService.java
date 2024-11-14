package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupMemberRepository;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupRepository;
import com.almagest_dev.tacobank_core_server.domain.settlememt.model.Settlement;
import com.almagest_dev.tacobank_core_server.domain.settlememt.model.SettlementDetails;
import com.almagest_dev.tacobank_core_server.domain.settlememt.repository.SettlementDetailsRepository;
import com.almagest_dev.tacobank_core_server.domain.settlememt.repository.SettlementRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.SettlementDetailsResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.SettlementRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.SettlementResponseDto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class SettlementService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementDetailsRepository settlementDetailsRepository;

    public SettlementService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
                             SettlementRepository settlementRepository, SettlementDetailsRepository settlementDetailsRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.settlementRepository = settlementRepository;
        this.settlementDetailsRepository = settlementDetailsRepository;
    }

    public List<SettlementResponseDto> calculateSettlement(SettlementRequestDto request) {
        // Fetch group information
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));

        // Fetch all ACCEPTED group members including the leader
        List<GroupMember> acceptedMembers = groupMemberRepository.findByPayGroupAndStatus(group, "ACCEPTED");

        // Calculate total members (including the leader, who is already in the acceptedMembers list)
        int totalMembers = acceptedMembers.size();
        int perMemberAmount = request.getTotalAmount() / totalMembers;

        // Create and save main Settlement record
        Settlement settlement = new Settlement();
        settlement.setPayGroup(group);
        settlement.setSettlementAccountId(request.getAccountId());
        settlement.setSettlementTotalAmount(request.getTotalAmount());
        settlement.setSettlementStatus("N");
        settlementRepository.save(settlement);

        // Create and save individual SettlementDetails records for each member
        List<SettlementResponseDto> settlementList = new ArrayList<>();
        System.out.println("Total members for settlement: " + acceptedMembers.size());

        for (GroupMember member : acceptedMembers) {
            System.out.println("Saving settlement detail for member ID: " + member.getMember().getId());
            SettlementDetails settlementDetail = new SettlementDetails();
            settlementDetail.setSettlement(settlement);
            settlementDetail.setGroupMember(member);
            settlementDetail.setSettlementAmount(perMemberAmount);
            settlementDetail.setSettlementStatus("N");
            settlementDetailsRepository.saveAndFlush(settlementDetail);

            // Add to settlement response list
            settlementList.add(new SettlementResponseDto(member.getMember().getId(), perMemberAmount));
        }

        return settlementList;
    }

    public List<SettlementDetailsResponseDto> getSettlementDetailsByGroupId(Long groupId) {
        List<SettlementDetails> settlementDetails = settlementDetailsRepository.findByGroupId(groupId);
        return settlementDetails.stream()
                .map(detail -> new SettlementDetailsResponseDto(
                        detail.getGroupMember().getMember().getId(),
                        detail.getSettlementAmount(),
                        detail.getSettlementStatus()))
                .collect(Collectors.toList());
    }

    public List<SettlementDetailsResponseDto> getSettlementDetailsForMember(Long groupId, Long memberId) {
        List<SettlementDetails> detailsList = settlementDetailsRepository.findByGroupMember_Member_IdAndGroupMember_PayGroup_Id(memberId, groupId);

        return detailsList.stream()
                .map(detail -> new SettlementDetailsResponseDto(
                        detail.getGroupMember().getMember().getId(), // 실제 memberId
                        detail.getSettlementAmount(),
                        detail.getSettlementStatus()
                ))
                .collect(Collectors.toList());
    }
}