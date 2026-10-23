package com.banking.transactionservice.repository;

import com.banking.transactionservice.model.Transaction;
import com.banking.transactionservice.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    @Query("SELECT t FROM Transaction t WHERE " +
            "(:from IS NULL OR t.createdAt >= :from) AND " +
            "(:to IS NULL OR t.createdAt <= :to) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:minAmount IS NULL OR t.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR t.amount <= :maxAmount) AND " +
            "(:accountNumber IS NULL OR t.senderAccountNumber = :accountNumber " +
            "   OR t.receiverAccountNumber = :accountNumber)")
    Page<Transaction> adminSearch(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("status") TransactionStatus status,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            @Param("accountNumber") String accountNumber,
            Pageable pageable);

    long countByStatus(TransactionStatus status);

    List<Transaction> findBySenderAccountNumberOrderByCreatedAtDesc(String accountNumber);

    long countByCreatedAtAfter(LocalDateTime start);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.createdAt >= :start AND t.status = 'COMPLETED'")
    BigDecimal sumAmountSince(@Param("start") LocalDateTime start);

}
