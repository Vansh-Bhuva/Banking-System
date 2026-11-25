package com.example.authservice.controller;

import com.example.authservice.dto.KycRejectRequest;
import com.example.authservice.enums.RequestStatus;
import com.example.authservice.security.CustomUserDetails;
import com.example.authservice.service.AccountRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// controller/AdminAccountRequestController.java
@RestController
@RequestMapping("/api/v1/admin/account-requests")
@RequiredArgsConstructor
public class AdminAccountRequestController {
    private final AccountRequestService service;

    private Long actorId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails u))
            throw new RuntimeException("Unauthenticated request");
        return u.getUser().getId();
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(defaultValue = "PENDING") RequestStatus status,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return service.list(status, page, size);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        service.approve(id, actorId(), "admin");
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id, @Valid @RequestBody KycRejectRequest req) {
        service.reject(id, req.getReason(), actorId(), "admin");
        return ResponseEntity.noContent().build();
    }
}