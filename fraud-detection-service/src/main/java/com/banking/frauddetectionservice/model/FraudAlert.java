package com.banking.frauddetectionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fraud_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String rule;          // VELOCITY, HIGH_AMOUNT, ROUND_NUMBER

    @Column(nullable = false)
    private String severity;      // LOW, MEDIUM, HIGH

    @Column(nullable = false)
    private String status;        // OPEN, DISMISSED, ACTION_TAKEN

    private String reviewedBy;
    private String notes;
    private LocalDateTime reviewedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}