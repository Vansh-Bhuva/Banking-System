package com.banking.transactionservice.controller;

import com.banking.transactionservice.dto.AdminTransactionResponse;
import com.banking.transactionservice.model.TransactionStatus;
import com.banking.transactionservice.service.AdminTransactionService;
import com.banking.transactionservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;
    private final AuditLogService auditLogService;

    private Long currentActor() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    @GetMapping("/audit")
    public ResponseEntity<Map<String, Object>> audit(
            @RequestParam(required = false) Long actor,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.search(actor, action, page, size));
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminTransactionService.search(
                from, to, status, minAmount, maxAmount, accountNumber, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminTransactionResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(adminTransactionService.get(id));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<AdminTransactionResponse> reverse(
            @PathVariable String id,
            @RequestParam String reason) {
        return ResponseEntity.ok(adminTransactionService.reverse(id, reason,currentActor()));
    }
}