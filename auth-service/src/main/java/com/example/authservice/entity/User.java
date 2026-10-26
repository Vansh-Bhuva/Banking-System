package com.example.authservice.entity;

import com.example.authservice.enums.KycStatus;
import com.example.authservice.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(length = 512)
    private String aadhaarEncrypted;   // note: encrypt in production

    @Column(length = 512)
    private String panEncrypted;

    // ── KYC ────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.PENDING;

    private LocalDateTime kycSubmittedAt;
    private LocalDateTime kycVerifiedAt;
    private LocalDate kycExpiryDate;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal ytdCredits = BigDecimal.ZERO;

    private Integer fyYear;

    // ── Admin block ────────────────────────────────
    @Column(nullable = false)
    @Builder.Default
    private Boolean blocked = false;

    private String kycRejectionReason;
}