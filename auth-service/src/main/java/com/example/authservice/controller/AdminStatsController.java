package com.example.authservice.controller;

import com.example.authservice.enums.KycStatus;
import com.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stats/users")
@RequiredArgsConstructor
public class AdminStatsController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> stats() {
        long total = userRepository.count();
        long blocked = userRepository.countByBlocked(true);
        long pendingKyc = userRepository.countByKycStatus(KycStatus.PENDING);
        long fullKyc = userRepository.countByKycStatus(KycStatus.FULL_KYC);

        return ResponseEntity.ok(Map.of(
                "totalUsers", total,
                "blockedUsers", blocked,
                "pendingKyc", pendingKyc,
                "fullKyc", fullKyc
        ));
    }
}