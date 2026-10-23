package com.banking.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "beneficiaries",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_account",
        columnNames = {"user_id", "account_number"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Beneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "beneficiary_name", nullable = false)
    private String beneficiaryName;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(length = 11)
    private String ifsc;

    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeneficiaryStatus status;

    @Column(name = "cooling_ends_at")
    private LocalDateTime coolingEndsAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;
}