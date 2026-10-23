package com.banking.transactionservice.service;

import com.banking.transactionservice.dto.AdminTransactionResponse;
import com.banking.transactionservice.model.Transaction;
import com.banking.transactionservice.model.TransactionStatus;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final AuditLogService auditLogService;

    public Map<String, Object> search(LocalDateTime from, LocalDateTime to,
                                      TransactionStatus status,
                                      BigDecimal minAmount, BigDecimal maxAmount,
                                      String accountNumber,
                                      int page, int size) {
        Pageable p = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        Page<Transaction> result = transactionRepository.adminSearch(
                from, to, status, minAmount, maxAmount, accountNumber, p);

        return Map.of(
                "content", result.getContent().stream().map(this::map).toList(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", page
        );
    }

    public AdminTransactionResponse get(String id) {
        return map(find(id));
    }

    @Transactional
    public AdminTransactionResponse reverse(String id, String reason, Long actorId) {
        Transaction t = find(id);

        if (t.getStatus() != TransactionStatus.COMPLETED) {
            throw new RuntimeException(
                    "Only COMPLETED transactions can be reversed. Current status: "
                            + t.getStatus());
        }
        String before = "status=" + t.getStatus();
        transactionService.adminReverse(t, reason);
        auditLogService.log(actorId, "admin", "REVERSE_TXN",
                "TRANSACTION", id, before, "status=REVERSED reason=" + reason, null);

        return map(transactionRepository.findById(id).orElseThrow());
    }

    private Transaction find(String id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + id));
    }

    private AdminTransactionResponse map(Transaction t) {
        AdminTransactionResponse r = new AdminTransactionResponse();
        r.setId(t.getId());
        r.setUserId(t.getUserId());
        r.setSenderAccountNumber(t.getSenderAccountNumber());
        r.setReceiverAccountNumber(t.getReceiverAccountNumber());
        r.setAmount(t.getAmount());
        r.setType(t.getType());
        r.setStatus(t.getStatus());
        r.setDescription(t.getDescription());
        r.setFailureReason(t.getFailureReason());
        r.setReferenceNumber(t.getReferenceNumber());
        r.setCreatedAt(t.getCreatedAt());
        r.setCompletedAt(t.getCompletedAt());
        return r;
    }
}