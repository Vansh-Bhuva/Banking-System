package com.banking.accountservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs",
       indexes = @Index(name = "idx_audit_created", columnList = "createdAt"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Long actorUserId;
    private String actorEmail;

    @Column(nullable = false)
    private String action;         // FREEZE_ACCOUNT, APPROVE_KYC, REVERSE_TXN

    @Column(nullable = false)
    private String targetType;     // USER, ACCOUNT, TRANSACTION

    @Column(nullable = false)
    private String targetId;

    @Column(length = 2000)
    private String beforeState;

    @Column(length = 2000)
    private String afterState;

    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime createdAt;
}