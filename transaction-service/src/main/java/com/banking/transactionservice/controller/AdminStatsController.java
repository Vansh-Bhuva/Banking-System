package com.banking.transactionservice.controller;

import com.banking.transactionservice.model.TransactionStatus;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stats/transactions")
@RequiredArgsConstructor
public class AdminStatsController {

    private final TransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> stats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        long total = transactionRepository.count();
        long completed = transactionRepository.countByStatus(TransactionStatus.COMPLETED);
        long reversed = transactionRepository.countByStatus(TransactionStatus.REVERSED);
        long todayCount = transactionRepository.countByCreatedAtAfter(startOfDay);
        BigDecimal todayVolume = transactionRepository.sumAmountSince(startOfDay);

        return ResponseEntity.ok(Map.of(
                "totalTransactions", total,
                "completedTransactions", completed,
                "reversedTransactions", reversed,
                "todayTransactions", todayCount,
                "todayVolume", todayVolume == null ? BigDecimal.ZERO : todayVolume
        ));
    }
}