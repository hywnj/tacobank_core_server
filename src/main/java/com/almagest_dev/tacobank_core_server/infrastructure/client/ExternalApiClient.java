package com.almagest_dev.tacobank_core_server.infrastructure.client;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import jakarta.transaction.Transaction;

import java.util.List;

public interface ExternalApiClient {
    List<Account> getAccounts(String userFinanceId);
    List<Transaction> getRecentTransactions(String userFinanceId);
}
