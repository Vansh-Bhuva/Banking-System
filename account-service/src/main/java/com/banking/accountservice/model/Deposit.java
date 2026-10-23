package com.banking.accountservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deposit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String accountNumber;   // linked savings/current account

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepositType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DepositStatus status;

    // FD: principal | RD: monthly installment amount
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal principal;

    @Column(nullable = false)
    private Integer tenureMonths;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;   // annual %, e.g. 7.25

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal maturityAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate maturityDate;

    // RD only
    private Integer installmentsPaid;
    private LocalDate nextDueDate;

    private Boolean autoRenew;

    private LocalDateTime closedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}