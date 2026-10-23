package com.banking.frauddetectionservice.repository;

import com.banking.frauddetectionservice.model.FraudAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, String> {

    Page<FraudAlert> findByStatus(String status, Pageable pageable);

    Page<FraudAlert> findBySeverity(String severity, Pageable pageable);

    Page<FraudAlert> findByStatusAndSeverity(String status, String severity, Pageable pageable);

    long countByStatus(String status);
}