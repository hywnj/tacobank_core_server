package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.model.MainAccount;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.account.repository.MainAccountRepository;
import com.almagest_dev.tacobank_core_server.domain.group.model.Group;
import com.almagest_dev.tacobank_core_server.domain.group.model.GroupMember;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupMemberRepository;
import com.almagest_dev.tacobank_core_server.domain.group.repository.GroupRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.domain.settlememt.model.Settlement;
import com.almagest_dev.tacobank_core_server.domain.settlememt.model.SettlementDetails;
import com.almagest_dev.tacobank_core_server.domain.settlememt.repository.SettlementDetailsRepository;
import com.almagest_dev.tacobank_core_server.domain.settlememt.repository.SettlementRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;

import com.almagest_dev.tacobank_core_server.presentation.dto.settlement.SettlementMemberDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.settlement.SettlementRequestDto;
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
    private final MemberRepository memberRepository;


    public SettlementService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
                             SettlementRepository settlementRepository, SettlementDetailsRepository settlementDetailsRepository,
                             MainAccountRepository mainAccountRepository, NotificationService notificationService, AccountRepository accountRepository, MemberRepository memberRepository) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.settlementRepository = settlementRepository;
        this.settlementDetailsRepository = settlementDetailsRepository;
        this.mainAccountRepository = mainAccountRepository;
        this.notificationService = notificationService;
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
    }
    
    public void processSettlementRequest(SettlementRequestDto request) {
        Group group;

        // 1. 그룹 확인 또는 생성
        if (request.getGroupId() == null) {
            // 친구 선택으로 그룹 생성
            group = createTemporaryGroup(request);
        } else {
            // 기존 그룹 조회
            group = groupRepository.findById(request.getGroupId())
                    .orElseThrow(() -> new IllegalArgumentException("그룹이 존재하지 않습니다."));
        }

        // 선택된 계좌 확인
        Account selectedAccount = accountRepository.findById(request.getSettlementAccountId())
                .orElseThrow(() -> new IllegalArgumentException("선택된 계좌가 존재하지 않습니다."));

        // 2. 정산 데이터 생성 및 저장
        Settlement settlement = new Settlement();
        settlement.setPayGroup(group);
        settlement.setSettlementAccount(selectedAccount);
        settlement.setSettlementTotalAmount(request.getTotalAmount());
        settlement.setSettlementStatus("N");
        settlementRepository.save(settlement);

        // 3. 개인 멤버별 정산 데이터 저장
        for (SettlementMemberDto memberDto : request.getMemberAmounts()) {
            GroupMember groupMember = groupMemberRepository.findByPayGroupAndMemberId(group, memberDto.getMemberId())
                    .orElseThrow(() -> new IllegalArgumentException("그룹에 해당 멤버가 존재하지 않습니다."));

            SettlementDetails settlementDetails = new SettlementDetails();
            settlementDetails.setSettlement(settlement);
            settlementDetails.setGroupMember(groupMember);
            settlementDetails.setSettlementAmount(memberDto.getAmount());
            settlementDetails.setSettlementStatus("N");
            settlementDetailsRepository.save(settlementDetails);

            // 알림 전송
            String message = String.format("정산 요청이 도착했습니다. 요청 금액: %d원", memberDto.getAmount());
            notificationService.sendNotification(groupMember.getMember(), message);
        }
    }

    private Group createTemporaryGroup(SettlementRequestDto request) {
        // 그룹장 찾기
        Member leader = groupRepository.findLeaderById(request.getLeaderId())
                .orElseThrow(() -> new IllegalArgumentException("그룹장을 찾을 수 없습니다."));

        // 그룹 생성
        Group group = new Group();
        group.setLeader(leader);
        group.setName(leader.getName() + "과 아이들");
        group.setActivated("N");
        group.setCustomized("N");
        groupRepository.save(group);

        // 친구 목록 추가
        for (Long friendId : request.getFriendIds()) {
            Member friend = memberRepository.findMemberById(friendId)
                    .orElseThrow(() -> new IllegalArgumentException("친구를 찾을 수 없습니다."));

            GroupMember groupMember = new GroupMember();
            groupMember.setPayGroup(group);
            groupMember.setMember(friend);
            groupMember.setStatus("ACCEPTED");
            groupMemberRepository.save(groupMember);
        }

        return group;
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
                        detail.getSettlementStatus(),
                        detail.getUpdatedDate()
                ))
                .collect(Collectors.toList());

        return new SettlementDetailsListResponseDto(settlement.getId(), settlementDetails);
    }

    public void notifyPendingSettlementForMember(Long settlementId, Long memberId) {
        // 특정 Settlement와 관련된 특정 Member의 정산 정보 조회
        SettlementDetails pendingDetail = settlementDetailsRepository.findBySettlement_IdAndGroupMember_Member_IdAndSettlementStatus(
                settlementId, memberId, "N"
        ).orElseThrow(() -> new IllegalStateException("해당 멤버의 정산 정보가 없거나 이미 완료된 정산입니다."));

        // 알림 전송
        String message = String.format("정산 요청이 아직 완료되지 않았습니다. 요청 금액: %d원", pendingDetail.getSettlementAmount());
        notificationService.sendNotification(pendingDetail.getGroupMember().getMember(), message);
    }

}