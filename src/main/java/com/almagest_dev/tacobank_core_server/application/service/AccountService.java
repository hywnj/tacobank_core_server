package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.account.model.MainAccount;
import com.almagest_dev.tacobank_core_server.domain.account.repository.MainAccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.AccountInfoDTO;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.IntegrateAccountRequestDto;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.IntegrateAccountResponseDto;
import com.almagest_dev.tacobank_core_server.infrastructure.client.testbed.TestbedApiClient;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final TestbedApiClient testbedApiClient;
    private final MainAccountRepository mainAccountRepository;
    private final TransferService transferService;


    public AccountService(MemberRepository memberRepository, AccountRepository accountRepository, TestbedApiClient testbedApiClient, MainAccountRepository mainAccountRepository, TransferService transferService) {
        this.memberRepository = memberRepository;
        this.accountRepository = accountRepository;
        this.testbedApiClient = testbedApiClient;
        this.mainAccountRepository = mainAccountRepository;
        this.transferService = transferService;
    }

    @Transactional
    public void setMainAccount(MainAccountRequestDto requestDto) {
        // Step 1: Member 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // Step 2: Account 조회 및 검증
        Account account = accountRepository.findByIdAndMember(requestDto.getAccountId(), member)
                .orElseThrow(() -> new IllegalArgumentException("해당 계좌가 존재하지 않거나 회원의 계좌가 아닙니다."));

        // Step 4: 새로운 메인 계좌 저장
        MainAccount mainAccount = new MainAccount();
        mainAccount.setMember(member);
        mainAccount.setAccount(account);
        mainAccountRepository.save(mainAccount);
    }

    @Transactional
    public AccountMemberReponseDto getUserAccounts(MemberRequestDto memberRequestDto) {
        // 멤버 조회
        Member member = memberRepository.findById(memberRequestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        String userFinanceId = member.getUserFinanceId();
        String userName = member.getName();
        IntegrateAccountResponseDto responseDto;

        if (userFinanceId == null || userFinanceId.isEmpty()) {
            // 최초 요청 - memberId를 기반으로 요청
            responseDto = fetchAccountsFromApi(memberRequestDto.getMemberId().toString(), userName);
            userFinanceId = responseDto.getUserFinanceId();

            // 응답받은 userFinanceId 저장
            member.setUserFinanceId(userFinanceId);
            memberRepository.save(member);
            System.out.println("Saved userFinanceId: " + userFinanceId);
        } else {
            // 이후 요청 - userFinanceId를 기반으로 요청
            responseDto = fetchAccountsFromApi(userFinanceId, userName);
        }

        // 계좌 정보 저장 (최초 1회만)
        if (accountRepository.countByMember(member) == 0) {
            saveAccounts(responseDto.getResList(), member);
            System.out.println("Accounts saved for member: " + member.getId());
        } else {
            System.out.println("Accounts already exist for member: " + member.getId());
        }

        // 최종 응답 생성
        return mapToMemberResponseDto(member, responseDto);
    }

    private IntegrateAccountResponseDto fetchAccountsFromApi(String userFinanceId, String userName) {
        IntegrateAccountRequestDto requestDto = new IntegrateAccountRequestDto();
        requestDto.setUserFinanceId(userFinanceId);
        requestDto.setUserName(userName);
        requestDto.setInquiryBankType("A");

        IntegrateAccountResponseDto responseDto = testbedApiClient.requestApi(
                requestDto, "/openbank/accounts", IntegrateAccountResponseDto.class
        );

        if (responseDto == null || responseDto.getResList() == null) {
            throw new IllegalArgumentException("Invalid API response");
        }

        return responseDto;
    }

    private void saveAccounts(List<AccountInfoDTO> accountInfoList, Member member) {
        List<Account> accounts = accountInfoList.stream().map(accountInfo -> {
            Account account = new Account();
            account.setMember(member);
            account.setAccountNumber(accountInfo.getAccountNum());
            account.setAccountHolderName(accountInfo.getFintechUseNum());
            account.setBankCode(accountInfo.getBankCodeStd());
            account.setFintechUseNum(accountInfo.getFintechUseNum());
            account.setVerificated("Y");
            return account;
        }).collect(Collectors.toList());

        accountRepository.saveAll(accounts);
    }

    private AccountMemberReponseDto mapToMemberResponseDto(Member member, IntegrateAccountResponseDto responseDto) {
        AccountMemberReponseDto response = new AccountMemberReponseDto();
        response.setMemberId(member.getId());
        response.setEmail(member.getEmail());
        response.setName(member.getName());
        response.setTel(member.getTel());

        // MainAccount 조회
        MainAccount mainAccount = mainAccountRepository.findByMember(member).orElse(null);
        response.setMainAccountId(mainAccount != null ? mainAccount.getAccount().getId() : null);

        // Account List 매핑
        List<AccountResponseDto> accountList = responseDto.getResList().stream()
                .map(accountInfo -> {
                    AccountResponseDto accountDto = new AccountResponseDto();
                    accountDto.setAccountId(
                            accountRepository.findByAccountNumber(accountInfo.getAccountNum())
                                    .map(Account::getId)
                                    .orElse(null)
                    );
                    accountDto.setAccountName(accountInfo.getProductName());
                    accountDto.setAccountNumber(accountInfo.getAccountNum());
                    accountDto.setAccountHolder(accountInfo.getFintechUseNum());
                    accountDto.setBankName(accountInfo.getBankCodeStd());
                    accountDto.setBalance(Double.valueOf(accountInfo.getBalanceAmt()));

                    // 거래 내역 추가
                    try {
                        List<TransactionResponseDto> transactionList =
                                transferService.getTransactionHistory(Long.valueOf(accountInfo.getAccountNum()));
                        accountDto.setTransactionList(transactionList);
                    } catch (Exception e) {
                        // 거래 내역 조회 실패 처리
                        accountDto.setTransactionList(List.of());
                    }

                    return accountDto;
                })
                .collect(Collectors.toList());

        response.setAccountList(accountList);
        return response;
    }
}