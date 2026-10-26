package com.example.authservice.dto;

import com.example.authservice.enums.KycStatus;
import com.example.authservice.enums.Role;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminUserResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private KycStatus kycStatus;
    private Boolean blocked;
    private String aadhaarMasked;   // "XXXXXXXX1234"
    private String panMasked;       // "XXXXX1234X"
    private BigDecimal ytdCredits;
    private LocalDateTime kycVerifiedAt;
    private String kycRejectionReason;
}