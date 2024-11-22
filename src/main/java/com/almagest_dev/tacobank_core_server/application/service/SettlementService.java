package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
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
import com.almagest_dev.tacobank_core_server.infrastructure.client.testbed.TestbedApiClient;

import com.almagest_dev.tacobank_core_server.presentation.dto.AccountBalance;
import com.almagest_dev.tacobank_core_server.presentation.dto.account.AccountDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.settlement.*;
import com.almagest_dev.tacobank_core_server.presentation.dto.testbed.BalanceInquiryApiRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.testbed.BalanceInquiryApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
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
    private final TestbedApiClient testbedApiClient;


    public SettlementService(GroupRepository groupRepository, GroupMemberRepository groupMemberRepository,
                             SettlementRepository settlementRepository, SettlementDetailsRepository settlementDetailsRepository,
                             MainAccountRepository mainAccountRepository, NotificationService notificationService, AccountRepository accountRepository, MemberRepository memberRepository, TestbedApiClient testbedApiClient) {
        this.groupRepository = groupRepository;
        this.groupMemberRepository = groupMemberRepository;
        this.settlementRepository = settlementRepository;
        this.settlementDetailsRepository = settlementDetailsRepository;
        this.mainAccountRepository = mainAccountRepository;
        this.notificationService = notificationService;
        this.accountRepository = accountRepository;
        this.memberRepository = memberRepository;
        this.testbedApiClient = testbedApiClient;
    }

    /**
     * 그룹 선택하여, 정산 요청하고 알림보내기
     */
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

    /**
     * 친구 선택시, 임시 그룹 만들기
     */
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


    /**
     * 정산 현황 조회하기
     */
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

    /**
     * 정산 상세 내역 조회하기
     */
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

    /**
     * 독촉 알림 보내기
     */
    public void notifyPendingSettlementForMember(Long settlementId, Long memberId) {
        // 특정 Settlement와 관련된 특정 Member의 정산 정보 조회
        SettlementDetails pendingDetail = settlementDetailsRepository.findBySettlement_IdAndGroupMember_Member_IdAndSettlementStatus(
                settlementId, memberId, "N"
        ).orElseThrow(() -> new IllegalStateException("해당 멤버의 정산 정보가 없거나 이미 완료된 정산입니다."));

        // 알림 전송
        String message = String.format("정산 요청이 아직 완료되지 않았습니다. 요청 금액: %d원", pendingDetail.getSettlementAmount());
        notificationService.sendNotification(pendingDetail.getGroupMember().getMember(), message);
    }

    /**
     * 바로 송금 - 정산 데이터 검증 & 사용자 전 계좌 잔액 조회
     */
    public SettlementTransferResponseDto validateSettlementsAndGetAvailableBalances(SettlementTransferRequestDto requestDto) {
        log.info("SettlementService::validateSettlementsAndGetAvailableBalances START");
        // 멤버 정보 확인
        Member member = memberRepository.findByIdAndDeleted(requestDto.getMemberId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 정산 정보 확인 (정산 상태가 N인 경우만 송금 할 수 있음)
        Settlement settlement = settlementRepository.findByIdAndSettlementStatus(requestDto.getSettlementId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 이미 완료된 정산입니다."));

        // 사용자의 그룹 멤버 여부 확인
        GroupMember groupMember = groupMemberRepository.findByPayGroupAndMemberId(settlement.getPayGroup(), requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("정산 그룹에 포함 되어있지 않습니다."));

        // 개별 정산 상세 확인
        SettlementDetails settlementDetails = settlementDetailsRepository.findBySettlement_IdAndGroupMember_Id(
                        settlement.getId(), groupMember.getId())
                .orElseThrow(() -> new IllegalArgumentException("정산 상세정보가 존재하지 않습니다."));
        // 개별 정산 상태 확인
        if (settlementDetails.getSettlementStatus().equals("Y")) {
            throw new IllegalArgumentException("이미 완료된 정산입니다. 거래 내역을 확인해보세요.");
        }
        // 정산 금액 일치여부 확인
        if (requestDto.getAmount() != settlementDetails.getSettlementAmount()) {
            throw new IllegalArgumentException("정산 금액이 상이합니다. 재시도하거나 관리자에게 문의해주세요.");
        }

        // 사용자의 출금 가능한 전 계좌 잔액 조회
        List<Account> availableAccounts = accountRepository.findByMember_IdAndVerificated(requestDto.getMemberId(), "Y");
        if (availableAccounts == null) { // 출금 가능한(본인 인증 완료한) 계좌가 없는 경우
            throw new IllegalStateException("출금할 수 있는 계좌가 없습니다. 출금을 위한 본인 인증을 진행해주세요.");
        }

        // TestBed 잔액조회 호출
        log.info("SettlementService::validateSettlementsAndGetAvailableBalances 다건 잔액 조회");
        List<AccountBalance> accountBalances = new ArrayList<>();
        for (Account account : availableAccounts) {
            BalanceInquiryApiRequestDto apiRequestDto = new BalanceInquiryApiRequestDto(
                    member.getUserFinanceId(),
                    account.getFintechUseNum(),
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            );
            // API 호출
            BalanceInquiryApiResponseDto apiResponse = testbedApiClient.requestApi(
                    apiRequestDto,
                    "/openbank/account",
                    BalanceInquiryApiResponseDto.class
            );
            log.info("SettlementService::validateSettlementsAndGetAvailableBalances 잔액 조회 Response: {} ", apiResponse);

            // 개별 계좌 잔액 조회 실패 - 해당 계좌 잔액 0원으로 Return
            if (apiResponse.getApiTranId() == null || !apiResponse.getRspCode().equals("A0000") || apiResponse.getBalanceAmt() == null) {
                log.warn("계좌 잔액 조회에 실패했습니다. - " + apiResponse.getRspMessage());
                accountBalances.add(new AccountBalance(account.getId(), 0));
                continue;
            }
            // 개별 계좌 잔액 조회 성공 - 계좌별 잔액 객체에 추가
            int balance = (apiResponse.getBalanceAmt() == null) ? 0 : Integer.parseInt(apiResponse.getBalanceAmt());
            accountBalances.add(new AccountBalance(account.getId(), balance));
        }
        if (accountBalances == null || accountBalances.size() == 0) {
            throw new IllegalArgumentException("출금 가능한 계좌의 잔액 조회 결과가 없습니다.");
        }

        // 응답 반환
        log.info("SettlementService::validateSettlementsAndGetAvailableBalances END");
        return new SettlementTransferResponseDto(requestDto.getIdempotencyKey(), accountBalances);
    }
}