package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.account.model.FavoriteAccount;
import com.almagest_dev.tacobank_core_server.domain.account.model.MainAccount;
import com.almagest_dev.tacobank_core_server.domain.account.repository.FavoriteAccountRepository;
import com.almagest_dev.tacobank_core_server.domain.account.repository.MainAccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.transfer.model.Transfer;
import com.almagest_dev.tacobank_core_server.domain.transfer.repository.TransferRepository;

import com.almagest_dev.tacobank_core_server.presentation.dto.account.AccountDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.account.MainAccountRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.transfer.TransferOptionsResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;
    private final MainAccountRepository mainAccountRepository;
    private final FavoriteAccountRepository favoriteAccountRepository;
    private final TransferRepository transferRepository;

    /**
     * 메인 계좌 설정
     */
    @Transactional
    public void setMainAccount(MainAccountRequestDto requestDto) {
        //Member 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        //Account 조회 및 검증
        Account account = accountRepository.findByIdAndMember(requestDto.getAccountId(), member)
                .orElseThrow(() -> new IllegalArgumentException("해당 계좌가 존재하지 않거나 회원의 계좌가 아닙니다."));

        //새로운 메인 계좌 저장
        MainAccount mainAccount = new MainAccount();
        mainAccount.saveMember(member);
        mainAccount.saveAccount(account);
        mainAccountRepository.save(mainAccount);
    }

    /**
     * 메인 계좌 수정
     */
    @Transactional
    public void updateMainAccount(MainAccountRequestDto requestDto) {
        // 기존 MainAccount 조회
        MainAccount mainAccount = mainAccountRepository.findByMemberId(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("메인 계좌가 존재하지 않습니다."));

        // 계좌 변경
        Account account = accountRepository.findByIdAndMember(requestDto.getAccountId(), mainAccount.getMember())
                .orElseThrow(() -> new IllegalArgumentException("해당 계좌가 존재하지 않거나 회원의 계좌가 아닙니다."));

        // 기존 엔티티 업데이트
        mainAccount.saveAccount(account);
        mainAccountRepository.save(mainAccount);
    }


    /**
     * 즐겨찾기, 최근 이체 계좌 조회
     */
    public TransferOptionsResponseDto getTransferOptions(Long memberId) {
        Member member = memberRepository.findByIdAndDeleted(memberId, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 즐겨찾기 계좌 (모두 조회)
        List<FavoriteAccount> favoriteAccounts = favoriteAccountRepository.findAllByMember(member);
        List<AccountDto> favoriteAccountDtos = favoriteAccounts.stream()
                .map(account -> new AccountDto(
                        account.getAccountHolderName(),
                        account.getAccountNum(),
                        account.getBankCode()
                ))
                .toList();

        // 타코뱅크 서비스에서 최근 이체한 내역 5개 조회
        Pageable pageable = PageRequest.of(0, 5); // 최근 5개
        List<Transfer> recentTransfers = transferRepository.findTop5DistinctByMemberIdAndStatus(memberId, "S", pageable);
        List<AccountDto> recentAccountDtos = recentTransfers.stream()
                .map(transfer -> new AccountDto(
                        transfer.getReceiverAccountHolder(),
                        transfer.getReceiverAccountNum(),
                        transfer.getReceiverBankCode()
                ))
                .toList();

        return new TransferOptionsResponseDto(favoriteAccountDtos, recentAccountDtos);
    }
}