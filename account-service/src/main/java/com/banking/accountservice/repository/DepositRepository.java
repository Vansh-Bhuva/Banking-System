package com.banking.accountservice.repository;

import com.banking.accountservice.model.Deposit;
import com.banking.accountservice.model.DepositStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DepositRepository extends JpaRepository<Deposit, String> {

    List<Deposit> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);

    List<Deposit> findByStatusAndMaturityDateLessThanEqual(
            DepositStatus status, LocalDate date);

    List<Deposit> findByStatusAndNextDueDateLessThanEqual(
            DepositStatus status, LocalDate date);
}