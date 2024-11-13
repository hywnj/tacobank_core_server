package com.almagest_dev.tacobank_core_server.infrastructure.client;

import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import jakarta.transaction.Transaction;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ExternalApiClientImpl implements ExternalApiClient {

    @Override
    public List<Account> getAccounts(String userFinanceId) {
        // 외부 API 호출 로직 구현
        return null;
    }

    @Override
    public List<Transaction> getRecentTransactions(String userFinanceId) {
        // 외부 API 호출 로직 구현
        return null;
    }
}
