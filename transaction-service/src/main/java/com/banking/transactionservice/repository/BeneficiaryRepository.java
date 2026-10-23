package com.banking.transactionservice.repository;

import com.banking.transactionservice.model.Beneficiary;
import com.banking.transactionservice.model.BeneficiaryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, String> {

    List<Beneficiary> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Beneficiary> findByUserIdAndAccountNumber(Long userId, String accountNumber);

    boolean existsByUserIdAndAccountNumber(Long userId, String accountNumber);

    List<Beneficiary> findByStatusAndCoolingEndsAtBefore(
            BeneficiaryStatus status, java.time.LocalDateTime cutoff);
}