package com.example.authservice.controller;

import com.example.authservice.dto.*;
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

    private Long currentUserId() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof com.example.authservice.security.CustomUserDetails cud) {
            return cud.getUser().getId();
        }
        if (p instanceof Long l) return l;
        throw new RuntimeException("Unauthenticated");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/password/change")
    public ResponseEntity<AuthResponse> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest req) {
        return ResponseEntity.ok(authService.changePassword(authHeader.replaceFirst("^Bearer ", ""), req));
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

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile() {
        Long userId = currentUserId();
        var u = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(mapToProfile(u));
    }

    private ProfileResponse mapToProfile(com.example.authservice.entity.User u) {
        ProfileResponse r = new ProfileResponse();
        r.setId(u.getId());
        r.setName(u.getName());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole());
        r.setKycStatus(u.getKycStatus());
        r.setBlocked(u.getBlocked());
        r.setKycSubmittedAt(u.getKycSubmittedAt());
        r.setKycVerifiedAt(u.getKycVerifiedAt());
        r.setKycRejectionReason(u.getKycRejectionReason());
        return r;
    }
}