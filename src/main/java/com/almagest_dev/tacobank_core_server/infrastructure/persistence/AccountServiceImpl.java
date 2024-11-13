package com.almagest_dev.tacobank_core_server.infrastructure.persistence;

import com.almagest_dev.tacobank_core_server.application.service.AccountService;
import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.model.FavoriteAccount;
import com.almagest_dev.tacobank_core_server.domain.account.model.MainAccount;
import com.almagest_dev.tacobank_core_server.domain.account.repository.AccountRepository;
import com.almagest_dev.tacobank_core_server.domain.account.repository.FavoriteAccountRepository;
import com.almagest_dev.tacobank_core_server.domain.account.repository.MainAccountRepository;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.client.ExternalApiClient;
import jakarta.transaction.Transaction;
import lombok.Getter;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    @Getter
    private final AccountRepository accountRepository;
    private final MainAccountRepository mainAccountRepository;
    private final FavoriteAccountRepository favoriteAccountRepository;
    private final MemberRepository memberRepository;
    private final ExternalApiClient externalApiClient; // 외부 API 클라이언트

    public AccountServiceImpl(AccountRepository accountRepository,
                              MainAccountRepository mainAccountRepository,
                              FavoriteAccountRepository favoriteAccountRepository,
                              MemberRepository memberRepository,
                              ExternalApiClient externalApiClient) {
        this.accountRepository = accountRepository;
        this.mainAccountRepository = mainAccountRepository;
        this.favoriteAccountRepository = favoriteAccountRepository;
        this.memberRepository = memberRepository;
        this.externalApiClient = externalApiClient;
    }


    public Long getCurrentUserId() {
        // 실제 인증이 추가되면 SecurityContextHolder에서 인증된 사용자 ID를 가져오는 로직으로 변경
        return 1L;
    }

    @Override
    public List<Account> getUserAccounts(String userFinanceId) {
        // 외부 API를 통해 사용자 계좌 정보 조회
        return externalApiClient.getAccounts(userFinanceId);
    }

    @Override
    public List<Transaction> getRecentTransactions(String userFinanceId) {
        // 외부 API를 통해 사용자 거래 내역 조회
        return externalApiClient.getRecentTransactions(userFinanceId);
    }

    @Override
    public MainAccount registerMainAccount(Long memberId, Account account) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        MainAccount mainAccount = new MainAccount();
        mainAccount.setMember(member);
        mainAccount.setAccount(account);
        mainAccountRepository.save(mainAccount);

        return mainAccount;
    }

    @Override
    public FavoriteAccount addFavoriteAccount(Long memberId, Account account) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        FavoriteAccount favoriteAccount = new FavoriteAccount();
        favoriteAccount.setMember(member);
        favoriteAccount.setAccountNumber(account.getAccountNumber());
        favoriteAccount.setAccountHolderName(account.getAccountHolderName());
        favoriteAccount.setBankCode(account.getBankCode());
        favoriteAccountRepository.save(favoriteAccount);

        return favoriteAccount;
    }

}