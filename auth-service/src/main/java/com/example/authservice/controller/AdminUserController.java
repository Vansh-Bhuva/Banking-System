package com.example.authservice.controller;

import com.example.authservice.dto.AdminUserResponse;
import com.example.authservice.dto.KycRejectRequest;
import com.example.authservice.service.AdminUserService;
import com.example.authservice.service.AuditLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final AuditLogService auditLogService;

    private Long currentActor() {
        // 'var' enables local variable type inference (Java 10+), letting the compiler infer the type and avoiding an explicit import.
        // Use 'Authentication' explicitly instead if targeting Java 8 or if your coding guidelines prefer declared types.

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.example.authservice.security.CustomUserDetails u)) {
            throw new RuntimeException("Unauthenticated request");
        }
        return u.getUser().getId();
    }

    // ─── Users ─────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminUserService.searchUsers(search, page, size));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<AdminUserResponse> getUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUser(id));
    }

    @PutMapping("/users/{id}/block")
    public ResponseEntity<AdminUserResponse> block(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.blockUser(id, currentActor(), "admin"));
    }

    @PutMapping("/users/{id}/unblock")
    public ResponseEntity<AdminUserResponse> unblock(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.unblockUser(id, currentActor(), "admin"));
    }

    // ─── KYC Queue ────────────────────────────────
    @GetMapping("/kyc/pending")
    public ResponseEntity<Map<String, Object>> pendingKyc(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminUserService.pendingKyc(page, size));
    }

    @PutMapping("/kyc/{userId}/approve")
    public ResponseEntity<AdminUserResponse> approve(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.approveKyc(userId, currentActor(), "admin"));
    }

    @PutMapping("/kyc/{userId}/reject")
    public ResponseEntity<AdminUserResponse> reject(
            @PathVariable Long userId,
            @Valid @RequestBody KycRejectRequest req) {

        return ResponseEntity.ok(adminUserService.rejectKyc(userId, req.getReason(), currentActor(), "admin"));
    }

    @GetMapping("/users/audit")
    public ResponseEntity<Map<String, Object>> audit(
            @RequestParam(required = false) Long actor,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(auditLogService.search(actor, action, page, size));
    }
}