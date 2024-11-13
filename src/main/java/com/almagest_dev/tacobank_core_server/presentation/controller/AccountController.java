package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.AccountService;
import com.almagest_dev.tacobank_core_server.domain.account.model.Account;
import com.almagest_dev.tacobank_core_server.domain.account.model.FavoriteAccount;
import com.almagest_dev.tacobank_core_server.domain.account.model.MainAccount;
import jakarta.transaction.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/core/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/{userFinanceId}/list")
    public List<Account> getUserAccounts(@PathVariable String userFinanceId) {
        return accountService.getUserAccounts(userFinanceId);
    }

    @GetMapping("/{userFinanceId}/transactions")
    public List<Transaction> getRecentTransactions(@PathVariable String userFinanceId) {
        return accountService.getRecentTransactions(userFinanceId);
    }

    @PostMapping("/main")
    public MainAccount registerMainAccount(@RequestBody Account account) {
        Long memberId = accountService.getCurrentUserId(); // 서비스에서 현재 사용자 ID 가져오기
        return accountService.registerMainAccount(memberId, account);
    }

    @PostMapping("/favorite")
    public FavoriteAccount addFavoriteAccount(@RequestBody Account account) {
        Long memberId = accountService.getCurrentUserId(); // 서비스에서 현재 사용자 ID 가져오기
        return accountService.addFavoriteAccount(memberId, account);
    }
}
