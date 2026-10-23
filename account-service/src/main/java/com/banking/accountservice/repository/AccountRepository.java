package com.banking.accountservice.repository;

import com.banking.accountservice.model.Account;
import com.banking.accountservice.model.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    boolean existsByEmail(String email);
    List<Account> findByUserId(Long userId);
    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    Page<Account> findByAccountNumberContainingOrAccountHolderNameContainingIgnoreCase(
            String accountNumber, String name, Pageable pageable);

    long countByStatus(AccountStatus status);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a")
    BigDecimal sumAllBalances();
}
