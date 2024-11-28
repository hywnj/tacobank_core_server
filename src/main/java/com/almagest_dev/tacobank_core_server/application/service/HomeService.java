package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.external.testbed.client.TestbedApiClient;
import com.almagest_dev.tacobank_core_server.infrastructure.external.testbed.dto.*;
import com.almagest_dev.tacobank_core_server.presentation.dto.home.AccountMemberReponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.home.AccountResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.home.AccountRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.home.TransactionResponseDto2;
import com.almagest_dev.tacobank_core_server.presentation.dto.transantion.TransactionListRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final TestbedApiClient testbedApiClient;
    private final OrgCodeService orgCodeService;

    /**
     * 사용자 계좌 및 거래 내역 조회
     */
    @Transactional
    public AccountMemberReponseDto getUserAccounts(AccountRequestDto requestDto) {
        // 멤버 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("멤버를 찾을 수 없습니다."));

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

        // 계좌 및 거래 내역 응답 생성
        return mapToAccountMemberResponseDto(member, accountResponseDto, requestDto.getFromDate(), requestDto.getToDate());
    }

    private IntegrateAccountApiResponseDto fetchAccountsFromApi(String userFinanceId, String userName) {
        IntegrateAccountApiRequestDto requestDto = new IntegrateAccountApiRequestDto();
        requestDto.setUserFinanceId(userFinanceId);
        requestDto.setUserName(userName);
        requestDto.setInquiryBankType("A");

        return testbedApiClient.requestApi(requestDto, "/openbank/accounts", IntegrateAccountApiResponseDto.class);
    }

    private void saveAccounts(List<AccountInfoDto> accountInfoList, Member member) {
        List<Account> accounts = accountInfoList.stream().map(accountInfo -> {
            Account account = new Account();
            account.saveMember(member);
            account.saveAccountNum(accountInfo.getAccountNum());
            account.saveAccountHolderName(accountInfo.getAccountHolder());
            account.saveBankCode(accountInfo.getBankCodeStd());
            account.saveFintechUseNum(accountInfo.getFintechUseNum());
            account.saveVerified();
            return account;
        }).collect(Collectors.toList());
        accountRepository.saveAll(accounts);
    }

    private AccountMemberReponseDto mapToAccountMemberResponseDto(Member member, IntegrateAccountApiResponseDto accountResponseDto, String fromDate, String toDate) {
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
        return response;
    }

    private List<TransactionResponseDto2> fetchTransactionList(TransactionListRequestDto requestDto) {
        // API 요청 DTO 생성
        TransactionListApiRequestDto apiRequestDto = new TransactionListApiRequestDto();
        apiRequestDto.setFintechUseNum(accountRepository.findByAccountNum(requestDto.getAccountNum())
                .orElseThrow(() -> new IllegalArgumentException("계좌가 존재하지 않습니다."))
                .getFintechUseNum());
        apiRequestDto.setInquiryType("A");
        apiRequestDto.setInquiryBase("D");
        apiRequestDto.setFromDate(requestDto.getFromDate());
        apiRequestDto.setFromTime("001000");
        apiRequestDto.setToDate(requestDto.getToDate());
        apiRequestDto.setToTime("240000");
        apiRequestDto.setSortOrder("D");
        apiRequestDto.setTranDtime("20241129120000");
        apiRequestDto.setDataLength("5"); // 기본값

        TransactionListApiResponseDto responseDto = testbedApiClient.requestApi(apiRequestDto, "/openbank/tranlist", TransactionListApiResponseDto.class);

        if (responseDto.getResList() == null || responseDto.getResList().isEmpty()) {
            return List.of();
        }

        return responseDto.getResList().stream()
                .map(transaction -> {
                    TransactionResponseDto2 transactionDto = new TransactionResponseDto2();
                    /// !!!!!!!!!!!!!!!!!!!!!!
                    transactionDto.setApiTranId(transaction.getApiTranId());
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
