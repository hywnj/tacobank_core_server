package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.model.MainAccount;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.account.repository.MainAccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.external.testbed.client.TestbedApiClient;
import com.almagest_dev.tacobank_core_server.infrastructure.external.testbed.dto.*;
import com.almagest_dev.tacobank_core_server.presentation.dto.home.AccountMemberReponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.home.AccountResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.home.TransactionResponseDto2;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionListRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final MainAccountRepository mainAccountRepository;

    private final TestbedApiClient testbedApiClient;
    private final OrgCodeService orgCodeService;

    /**
     * 사용자 계좌 및 거래 내역 조회
     */
    @Transactional
    public AccountMemberReponseDto getMemberHome() {
        /**
         * 인증 정보로 멤버 조회
         */
        // 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("인증되지 않은 사용자입니다.");
        }

        // 인증 정보에서 멤버 ID 추출
        Long memberId = (Long) authentication.getDetails();
        log.info("HomeService::getMemberHome - memberId: {} ", memberId);

        // 멤버 조회
        Member member = memberRepository.findByIdAndDeleted(memberId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        String userFinanceId = member.getUserFinanceId();
        String userName = member.getName();
        IntegrateAccountApiResponseDto accountResponseDto;

        if (userFinanceId == null || userFinanceId.isEmpty()) {
            // 최초 요청 - memberId 기반으로 계좌 조회
            accountResponseDto = fetchAccountsFromApi(member.getId().toString(), userName);
            member.setUserFinanceId(accountResponseDto.getUserFinanceId());
            memberRepository.save(member);
        } else {
            // 이후 요청 - userFinanceId 기반으로 계좌 조회
            accountResponseDto = fetchAccountsFromApi(userFinanceId, userName);
        }

        // 계좌 정보 저장 (최초 요청 시)
        if (accountRepository.countByMember(member) == 0) {
            saveAccounts(accountResponseDto.getResList(), member);
        }

        // 현재 시점 및 포맷팅 설정
        String fromDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String toDate = LocalDateTime.now().plusMonths(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String tranDtime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 계좌 및 거래 내역 응답 생성
        return mapToAccountMemberResponseDto(member, accountResponseDto, fromDate, toDate, tranDtime);
    }

    /**
     * 테스트베드 요청: 오픈뱅킹 계좌 연동(조회)
     */
    private IntegrateAccountApiResponseDto fetchAccountsFromApi(String userFinanceId, String userName) {
        IntegrateAccountApiRequestDto requestDto = new IntegrateAccountApiRequestDto();
        requestDto.setUserFinanceId(userFinanceId);
        requestDto.setUserName(userName);
        requestDto.setInquiryBankType("A");

        return testbedApiClient.requestApi(requestDto, "/openbank/accounts", IntegrateAccountApiResponseDto.class);
    }

    /**
     * 사용자 Account 조회 결과 저장
     */
    private void saveAccounts(List<AccountInfoDto> accountInfoList, Member member) {
        List<Account> accounts = accountInfoList.stream().map(accountInfo -> {
            Account account = new Account();
            account.saveMember(member);
            account.saveAccountNum(accountInfo.getAccountNum());
            account.saveAccountHolderName(accountInfo.getAccountHolder());
            account.saveBankCode(accountInfo.getBankCodeStd());
            account.saveFintechUseNum(accountInfo.getFintechUseNum());
            account.saveVerified();
            account.saveAccountName(accountInfo.getProductName());

            return account;
        }).collect(Collectors.toList());
        accountRepository.saveAll(accounts);
    }

    /**
     * 홈 화면 응답 데이터로 변환/매핑
     */
    private AccountMemberReponseDto mapToAccountMemberResponseDto(Member member, IntegrateAccountApiResponseDto accountResponseDto, String fromDate, String toDate, String tranDtime) {
        AccountMemberReponseDto response = new AccountMemberReponseDto();
        response.setMemberId(member.getId());
        response.setEmail(member.getEmail());
        response.setName(member.getName());
        response.setTel(member.getTel());

        boolean isPinSet = member.getTransferPin() != null;
        response.setPinSet(isPinSet);

        List<AccountResponseDto> accountList = accountResponseDto.getResList().stream()
                .map(accountInfo -> {
                    AccountResponseDto accountDto = new AccountResponseDto();
                    accountDto.setAccountId(accountRepository.findByAccountNum(accountInfo.getAccountNum())
                            .map(Account::getId).orElse(null));
                    accountDto.setAccountName(accountInfo.getProductName());
                    accountDto.setBalance(Double.parseDouble(accountInfo.getBalanceAmt()));
                    accountDto.setBankCode(accountInfo.getBankCodeStd());
                    String bankName = orgCodeService.getBankNameByCode(accountInfo.getBankCodeStd());
                    accountDto.setBankName(bankName);
                    accountDto.setAccountNum(accountInfo.getAccountNum());
                    accountDto.setAccountHolder(accountInfo.getAccountHolder());

                    // 거래 내역 추가
                    TransactionListRequestDto transactionRequest = new TransactionListRequestDto();
                    transactionRequest.setAccountNum(accountInfo.getAccountNum());
                    transactionRequest.setFromDate(fromDate);
                    transactionRequest.setToDate(toDate);
                    List<TransactionResponseDto2> transactions = fetchTransactionList(transactionRequest);
                    accountDto.setTransactionList(transactions);

                    return accountDto;
                })
                .collect(Collectors.toList());

        response.setAccountList(accountList);

        // 메인 계좌 추가
        MainAccount mainAccount = mainAccountRepository.findByMemberId(member.getId()).orElse(null);
        Long mainAccountId = (mainAccount != null) ? mainAccount.getAccount().getId() : accountList.get(0).getAccountId(); // 없는 경우, 가장 상위 계좌로 세팅
        response.setMainAccountId(mainAccountId);

        return response;
    }

    /**
     * 테스트베드 요청: 거래내역 조회
     */
    private List<TransactionResponseDto2> fetchTransactionList(TransactionListRequestDto requestDto) {
        // 현재 시점
        LocalDateTime now = LocalDateTime.now();

        // fromDate: 1년 전 시점
        String fromDate = now.minusYears(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // toDate: 현재 시점
        String toDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // tranDtime: 현재 날짜와 시간
        String tranDtime = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // API 요청 DTO 생성
        TransactionListApiRequestDto apiRequestDto = new TransactionListApiRequestDto();
        apiRequestDto.setFintechUseNum(accountRepository.findByAccountNum(requestDto.getAccountNum())
                .orElseThrow(() -> new IllegalArgumentException("계좌가 존재하지 않습니다."))
                .getFintechUseNum());
        apiRequestDto.setInquiryType("A");
        apiRequestDto.setInquiryBase("D");
        apiRequestDto.setFromDate(fromDate);
        apiRequestDto.setFromTime("001000");
        apiRequestDto.setToDate(toDate);
        apiRequestDto.setToTime("240000");
        apiRequestDto.setSortOrder("D");
        apiRequestDto.setTranDtime(tranDtime); // 동적으로 설정된 트랜잭션 시간 추가
        apiRequestDto.setDataLength("5"); // 기본값


        TransactionListApiResponseDto responseDto = testbedApiClient.requestApi(apiRequestDto, "/openbank/tranlist", TransactionListApiResponseDto.class);

        if (responseDto.getResList() == null || responseDto.getResList().isEmpty()) {
            return List.of();
        }

        return responseDto.getResList().stream()
                .map(transaction -> {
                    TransactionResponseDto2 transactionDto = new TransactionResponseDto2();
                    transactionDto.setTranNum(transaction.getTranNum());
                    transactionDto.setPrintContent(transaction.getPrintContent());

                    // 출금(송금 포함)의 경우 금액을 음수로 변경
                    if ("출금".equals(transaction.getPrintContent()) || "송금 출금".equals(transaction.getPrintContent())) {
                        transactionDto.setTranAmt("-" + transaction.getTranAmt());
                    } else {
                        transactionDto.setTranAmt(transaction.getTranAmt());
                    }

                    transactionDto.setTranDateTime(transaction.getTranDate() + " " + transaction.getTranTime());
                    return transactionDto;
                })
                .collect(Collectors.toList());
        }
}
