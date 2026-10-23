package com.banking.accountservice.controller;

import com.banking.accountservice.dto.AdminAccountResponse;
import com.banking.accountservice.model.AccountStatus;
import com.banking.accountservice.service.AdminAccountService;
import com.banking.accountservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminAccountService adminAccountService;
    private final AuditLogService auditLogService;

    private Long currentActor() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminAccountService.searchAccounts(search, status, page, size));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AdminAccountResponse> get(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(adminAccountService.getAccount(accountNumber));
    }

    @PutMapping("/{accountNumber}/freeze")
    public ResponseEntity<AdminAccountResponse> freeze(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(adminAccountService.freeze(accountNumber, currentActor()));
    }

    @PutMapping("/{accountNumber}/unfreeze")
    public ResponseEntity<AdminAccountResponse> unfreeze(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(adminAccountService.unfreeze(accountNumber, currentActor()));
    }

    @PutMapping("/{accountNumber}/dormant")
    public ResponseEntity<AdminAccountResponse> dormant(
            @PathVariable String accountNumber) {
        return ResponseEntity.ok(adminAccountService.markDormant(accountNumber, currentActor()));
    }

    @GetMapping("/audit")
    public ResponseEntity<Map<String, Object>> audit(
            @RequestParam(required = false) Long actor,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.search(actor, action, page, size));
    }
}