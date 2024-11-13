package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.model.FavoriteAccount;
import com.almagest_dev.tacobank_core_server.domain.account.model.MainAccount;
import jakarta.transaction.Transaction;

import java.util.List;

public interface AccountService {
    Long getCurrentUserId();
    List<Account> getUserAccounts(String userFinanceId);
    List<Transaction> getRecentTransactions(String userFinanceId);
    MainAccount registerMainAccount(Long memberId, Account account);
    FavoriteAccount addFavoriteAccount(Long memberId, Account account);
}
