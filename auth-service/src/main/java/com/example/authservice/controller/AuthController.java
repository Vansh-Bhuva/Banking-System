package com.example.authservice.controller;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.KycSubmitRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.enums.KycStatus;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtService;
import com.example.authservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${internal.key}")
    private String expectedInternalKey;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/internal/users/{userId}/email")
    public ResponseEntity<String> getUserEmail(@PathVariable Long userId) {
        return ResponseEntity.ok(
                userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User not found"))
                        .getEmail()
        );
    }

    @GetMapping("/internal/users/{userId}/kyc")
    public ResponseEntity<Map<String, Object>> getKycStatus(@PathVariable Long userId) {
        var u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(Map.of(
                "kycStatus", u.getKycStatus().name(),
                "submittedAt", String.valueOf(u.getKycSubmittedAt()),
                "verifiedAt", String.valueOf(u.getKycVerifiedAt())
        ));
    }

    @PostMapping("/kyc/submit")
    public ResponseEntity<Map<String, Object>> submitKyc(
            @Valid @RequestBody KycSubmitRequest req) {

        Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        Long userId;
        if (principal instanceof com.example.authservice.security.CustomUserDetails u) {
            userId = u.getUser().getId();
        } else if (principal instanceof Long l) {
            userId = l;
        } else {
            throw new RuntimeException("Unauthenticated request");
        }

        var u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (u.getKycStatus() == KycStatus.FULL_KYC) {
            throw new RuntimeException("KYC already verified");
        }
        if (u.getKycSubmittedAt() != null && u.getKycStatus() == KycStatus.PENDING) {
            throw new RuntimeException("KYC already submitted — awaiting review");
        }

        u.setAadhaarEncrypted(req.getAadhaar());
        u.setPanEncrypted(req.getPan());
        u.setKycSubmittedAt(LocalDateTime.now());
        u.setKycStatus(KycStatus.PENDING);
        userRepository.save(u);

        return ResponseEntity.ok(Map.of(
                "message", "KYC submitted — pending admin review",
                "kycStatus", u.getKycStatus().name(),
                "submittedAt", u.getKycSubmittedAt().toString()
        ));
    }

    @GetMapping("/users/{userId}/kyc")
    public ResponseEntity<?> getKyc(@PathVariable Long userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "kycStatus", user.getKycStatus().name(),
                "submittedAt", String.valueOf(user.getKycSubmittedAt()),
                "verifiedAt", String.valueOf(user.getKycVerifiedAt())
        ));
    }

    @PostMapping("/internal/service-token")
    public ResponseEntity<Map<String, String>> issueServiceToken(
            @RequestHeader(value = "X-Internal-Key", required = false) String internalKey) {

        if (!expectedInternalKey.equals(internalKey)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "forbidden"));
        }

        String token = jwtService.generateServiceToken(
                0L, "system@bank.local", "System", "ADMIN");

        return ResponseEntity.ok(Map.of("token", token));
    }
}