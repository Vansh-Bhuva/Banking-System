package com.banking.accountservice.controller;

import com.banking.accountservice.model.AccountStatus;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stats/accounts")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AccountRepository accountRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> stats() {
        long total = accountRepository.count();
        long active = accountRepository.countByStatus(AccountStatus.ACTIVE);
        long frozen = accountRepository.countByStatus(AccountStatus.FROZEN);
        long dormant = accountRepository.countByStatus(AccountStatus.DORMANT);
        BigDecimal totalBalance = accountRepository.sumAllBalances();

        return ResponseEntity.ok(Map.of(
                "totalAccounts", total,
                "activeAccounts", active,
                "frozenAccounts", frozen,
                "dormantAccounts", dormant,
                "totalDeposits", totalBalance == null ? BigDecimal.ZERO : totalBalance
        ));
    }
}