package com.banking.transactionservice.repository;
import com.banking.transactionservice.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    Page<AuditLog> findByActorUserIdOrderByCreatedAtDesc(Long actorUserId, Pageable p);
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable p);
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable p);
}