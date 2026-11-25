package com.example.authservice.entity;

import com.example.authservice.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_requests")
@Getter @Setter @NoArgsConstructor
public class AccountRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    @Column(nullable = false) private String phone;
    private LocalDate dateOfBirth;
    @Column(length = 512) private String address;
    @Column(length = 512) private String aadhaar;   // same handling as User today; encrypt later
    @Column(length = 512) private String pan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    private boolean emailVerified = false;
    private LocalDateTime submittedAt = LocalDateTime.now();
    private LocalDateTime reviewedAt;
    private Long reviewedBy;
    private String rejectionReason;
}