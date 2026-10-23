package com.banking.accountservice.service;

import com.banking.accountservice.dto.AdminAccountResponse;
import com.banking.accountservice.model.Account;
import com.banking.accountservice.model.AccountStatus;
import com.banking.accountservice.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAccountService {

    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    public Map<String, Object> searchAccounts(String search,
                                              AccountStatus status,
                                              int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Account> result;

        if (search != null && !search.isBlank()) {
            result = accountRepository
                    .findByAccountNumberContainingOrAccountHolderNameContainingIgnoreCase(
                            search, search, p);
        } else if (status != null) {
            result = accountRepository.findByStatus(status, p);
        } else {
            result = accountRepository.findAll(p);
        }

        return Map.of(
                "content", result.getContent().stream().map(this::map).toList(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", page
        );
    }

    public AdminAccountResponse getAccount(String accountNumber) {
        return map(find(accountNumber));
    }

    @Transactional
    public AdminAccountResponse freeze(String accountNumber, Long actorId) {
        Account a = find(accountNumber);
        String before = "status=" + a.getStatus();
        a.setStatus(AccountStatus.FROZEN);
        accountRepository.save(a);
        auditLogService.log(actorId, "admin", "FREEZE_ACCOUNT",
                "ACCOUNT", accountNumber, before, "status=FROZEN", null);
        return map(a);
    }

    @Transactional
    public AdminAccountResponse unfreeze(String accountNumber, Long actorId) {
        Account a = find(accountNumber);
        String before = "status=" + a.getStatus();
        a.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(a);
        auditLogService.log(actorId, "admin", "UNFREEZE_ACCOUNT",
                "ACCOUNT", accountNumber, before, "status=ACTIVE", null);
        return map(a);
    }

    @Transactional
    public AdminAccountResponse markDormant(String accountNumber, Long actorId) {
        Account a = find(accountNumber);
        String before = "status=" + a.getStatus();
        a.setStatus(AccountStatus.DORMANT);
        accountRepository.save(a);
        auditLogService.log(actorId, "admin", "MARK_DORMANT",
                "ACCOUNT", accountNumber, before, "status=DORMANT", null);
        return map(a);
    }

    private Account find(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Account not found: " + accountNumber));
    }

    private AdminAccountResponse map(Account a) {
        AdminAccountResponse r = new AdminAccountResponse();
        r.setId(a.getId());
        r.setAccountNumber(a.getAccountNumber());
        r.setUserId(a.getUserId());
        r.setAccountHolderName(a.getAccountHolderName());
        r.setEmail(a.getEmail());
        r.setPhone(a.getPhone());
        r.setAccountType(a.getAccountType());
        r.setStatus(a.getStatus());
        r.setBalance(a.getBalance());
        r.setDailyTransactionLimit(a.getDailyTransactionLimit());
        r.setDailyTransferred(a.getDailyTransferred());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        return r;
    }
}