package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.model.MainAccount;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.account.repository.MainAccountRepository;
import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupMemberRepository;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupRepository;
import com.almagest_dev.tacobank_core_server.domain.settlememt.model.Settlement;
import com.almagest_dev.tacobank_core_server.domain.settlememt.model.SettlementDetails;
import com.almagest_dev.tacobank_core_server.domain.settlememt.repository.SettlementDetailsRepository;
import com.almagest_dev.tacobank_core_server.domain.settlememt.repository.SettlementRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;

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
    private final MainAccountRepository mainAccountRepository;
    private final NotificationService notificationService;
    private final AccountRepository accountRepository;


    public SettlementService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
                             SettlementRepository settlementRepository, SettlementDetailsRepository settlementDetailsRepository,
                             MainAccountRepository mainAccountRepository, NotificationService notificationService, AccountRepository accountRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.settlementRepository = settlementRepository;
        this.settlementDetailsRepository = settlementDetailsRepository;
        this.mainAccountRepository = mainAccountRepository;
        this.notificationService = notificationService;
        this.accountRepository = accountRepository;

    }

    private Long getMainAccountIdForMember(Long memberId) {
        return mainAccountRepository.findByMemberId(memberId)
                .map(MainAccount::getAccount)
                .map(Account::getId)
                .orElse(null);
    }


    public List<SettlementResponseDto> calculateSettlement(SettlementRequestDto request) {
        // Fetch group information
        Group group = groupRepository.findById(request.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));

        // Fetch all ACCEPTED group members including the leader
        List<GroupMember> acceptedMembers = groupMemberRepository.findByPayGroupAndStatus(group, "ACCEPTED");

        Long leaderId = group.getLeader().getId();
        Account leaderMainAccount = accountRepository.findById(getMainAccountIdForMember(leaderId))
                .orElseThrow(() -> new IllegalStateException("그룹장의 메인 계좌를 찾을 수 없습니다."));

        int totalMembers = acceptedMembers.size()+1;
        int perMemberAmount = request.getTotalAmount() / totalMembers;

        Settlement settlement = new Settlement();
        settlement.setPayGroup(group);
        settlement.setSettlementAccount(leaderMainAccount);
        settlement.setSettlementTotalAmount(request.getTotalAmount());
        settlement.setSettlementStatus("N");
        settlementRepository.save(settlement);

        List<SettlementResponseDto> settlementList = new ArrayList<>();

        for (GroupMember member : acceptedMembers) {
            System.out.println("Saving settlement detail for member ID: " + member.getMember().getId());
            SettlementDetails settlementDetail = new SettlementDetails();
            settlementDetail.setSettlement(settlement);
            settlementDetail.setGroupMember(member);
            settlementDetail.setSettlementAmount(perMemberAmount);
            settlementDetail.setSettlementStatus("N");
            settlementDetailsRepository.saveAndFlush(settlementDetail);

            String message = String.format("정산 요청이 도착했습니다. 요청 금액: %d원", perMemberAmount);
            notificationService.sendNotification(member.getMember(), message);

            settlementList.add(new SettlementResponseDto(member.getMember().getId(), perMemberAmount, member.getMember().getName()));
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
                        detail.getGroupMember().getMember().getId(),
                        detail.getSettlementAmount(),
                        detail.getSettlementStatus()
                ))
                .collect(Collectors.toList());
    }

    public SettlementStatusResponseDto getSettlementStatus(Long memberId) {
        // 내가 만든 정산 리스트
        List<MyCreatedSettlementDto> createdSettlements = settlementRepository.findByPayGroup_Leader_Id(memberId).stream()
                .map(settlement -> new MyCreatedSettlementDto(
                        settlement.getId(),
                        settlement.getPayGroup().getId(),
                        settlement.getPayGroup().getName(),
                        settlement.getSettlementTotalAmount().longValue(),
                        settlement.getSettlementStatus(),
                        settlement.getCreatedDate(),
                        settlement.getUpdatedDate()
                ))
                .collect(Collectors.toList());

        // 내가 포함된 정산 리스트
        List<MyIncludedSettlementDto> includedSettlements = settlementDetailsRepository.findByGroupMember_Member_Id(memberId).stream()
                .map(detail -> new MyIncludedSettlementDto(
                        detail.getSettlement().getId(),
                        detail.getSettlement().getCreatedDate(),
                        detail.getSettlementAmount().longValue(),
                        detail.getSettlementStatus(),
                        new AccountDto(
                                detail.getSettlement().getSettlementAccount().getAccountHolderName(),
                                detail.getSettlement().getSettlementAccount().getAccountNumber(),
                                detail.getSettlement().getSettlementAccount().getBankCode()
                        )
                ))
                .collect(Collectors.toList());

        // 송금 가능한 계좌 리스트
        List<AccountDto> availableAccounts = accountRepository.findByMember_Id(memberId).stream()
                .map(account -> new AccountDto(
                        account.getAccountHolderName(),
                        account.getAccountNumber(),
                        account.getBankCode()
                ))
                .collect(Collectors.toList());

        return new SettlementStatusResponseDto(createdSettlements, includedSettlements, availableAccounts);
    }

    public SettlementDetailsListResponseDto getSettlementDetails(Long settlementId, Long memberId) {
        // Settlement ID에 해당하는 정산 데이터 가져오기
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new IllegalArgumentException("정산 ID에 해당하는 정산을 찾을 수 없습니다."));

        // 정산 세부 항목 가져오기
        List<SettlementDetails> detailsList = settlementDetailsRepository.findBySettlement_Id(settlementId);

        // DTO 변환
        List<SettlementDetailsResponseDto2> settlementDetails = detailsList.stream()
                .map(detail -> new SettlementDetailsResponseDto2(
                        detail.getGroupMember().getMember().getId(),
                        detail.getGroupMember().getMember().getName(),
                        detail.getSettlementAmount().longValue(),
                        detail.getSettlementStatus()
                ))
                .collect(Collectors.toList());

        return new SettlementDetailsListResponseDto(settlement.getId(), settlementDetails);
    }

    public void notifyPendingSettlements(Long settlementId) {

        List<SettlementDetails> pendingDetails = settlementDetailsRepository.findBySettlement_IdAndSettlementStatus(settlementId, "N");

        if (pendingDetails.isEmpty()) {
            throw new IllegalStateException("정산 완료되지 않은 사용자가 없습니다.");
        }

        for (SettlementDetails detail : pendingDetails) {
            String message = String.format("정산 요청이 아직 완료되지 않았습니다. 요청 금액: %d원", detail.getSettlementAmount());
            notificationService.sendNotification(detail.getGroupMember().getMember(), message);
        }
    }

}