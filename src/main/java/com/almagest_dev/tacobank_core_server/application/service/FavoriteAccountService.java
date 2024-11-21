package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.model.FavoriteAccount;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.account.repository.FavoriteAccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.FavoriteAccountRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.FavoriteAccountResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteAccountService {

    private final FavoriteAccountRepository favoriteAccountRepository;
    private final MemberRepository memberRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public FavoriteAccountResponseDto setAndRetrieveFavoriteAccount(FavoriteAccountRequestDto requestDto) {
        // Step 1: Member 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // Step 2: Account 조회
        Account account = accountRepository.findByAccountNumber(requestDto.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("해당 계좌가 존재하지 않습니다."));

        // Step 3: 중복된 즐겨찾기 계좌인지 확인
        boolean isDuplicate = favoriteAccountRepository.existsByMemberAndAccountNumber(member, account.getAccountNumber());
        if (isDuplicate) {
            throw new IllegalArgumentException("이미 즐겨찾기 계좌로 등록된 계좌입니다.");
        }

        Member accountOwner = memberRepository.findById(account.getMember().getId())
                .orElseThrow(() -> new IllegalArgumentException("계좌 소유자가 존재하지 않습니다."));

        // Step 4: FavoriteAccount 저장
        FavoriteAccount favoriteAccount = favoriteAccountRepository.findByMemberAndAccountNumber(member, requestDto.getAccountNumber())
                .orElse(new FavoriteAccount());
        favoriteAccount.setMember(member);
        favoriteAccount.setAccountNumber(account.getAccountNumber());
        favoriteAccount.setAccountHolderName(accountOwner.getName()); // 소유자의 이름을 저장
        favoriteAccount.setBankCode(account.getBankCode());
        favoriteAccount.setCreatedDate(LocalDateTime.now());
        favoriteAccount.setUpdatedDate(LocalDateTime.now());

        favoriteAccountRepository.save(favoriteAccount);


        // Step 5: 응답 생성
        return new FavoriteAccountResponseDto(
                member.getId(),
                favoriteAccount.getAccountNumber(),
                favoriteAccount.getAccountHolderName(),
                favoriteAccount.getBankCode()
        );

    }

    @Transactional
    public List<FavoriteAccountResponseDto> getFavoriteAccounts(Long memberId) {
        // Step 1: Member 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // Step 2: 즐겨찾기 계좌 리스트 조회
        List<FavoriteAccount> favoriteAccounts = favoriteAccountRepository.findAllByMember(member);

        // Step 3: 응답 생성
        return favoriteAccounts.stream().map(favoriteAccount -> new FavoriteAccountResponseDto(
                member.getId(),
                favoriteAccount.getAccountNumber(),
                favoriteAccount.getAccountHolderName(),
                favoriteAccount.getBankCode()
        )).collect(Collectors.toList());
    }

    @Transactional
    public void cancelFavoriteAccount(FavoriteAccountRequestDto requestDto) {
        // Step 1: Member 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // Step 2: FavoriteAccount 조회
        FavoriteAccount favoriteAccount = favoriteAccountRepository.findByMemberAndAccountNumber(member, requestDto.getAccountNumber())
                .orElseThrow(() -> new IllegalArgumentException("즐겨찾기에 등록되지 않은 계좌입니다."));

        // Step 3: 즐겨찾기 계좌 삭제
        favoriteAccountRepository.delete(favoriteAccount);
    }


}