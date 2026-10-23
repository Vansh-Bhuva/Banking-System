package com.example.authservice.entity;

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
    private String action;         // FREEZE_ACCOUNT, UNFREEZE_ACCOUNT, MARK_DORMANT

    @Column(nullable = false)
    private String targetType;     // ACCOUNT

    @Column(nullable = false)
    private String targetId;       // ACCOUNT NUMBER

    @Column(length = 2000)
    private String beforeState;

    @Column(length = 2000)
    private String afterState;

    private String ipAddress;

    @CreationTimestamp
    private LocalDateTime createdAt;
}