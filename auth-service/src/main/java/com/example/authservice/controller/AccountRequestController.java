package com.example.authservice.controller;

import com.example.authservice.dto.AccountRequestDto;
import com.example.authservice.dto.VerifyEmailDto;
import com.example.authservice.service.AccountRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/account-requests")
@RequiredArgsConstructor
public class AccountRequestController {
    private final AccountRequestService service;

    @PostMapping
    public ResponseEntity<Map<String, String>> submit(@Valid @RequestBody AccountRequestDto dto) {
        service.submit(dto);
        return ResponseEntity.accepted().body(Map.of(
                "message", "If the details are valid, a verification code has been sent to your email."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verify(@Valid @RequestBody VerifyEmailDto dto) {
        service.verifyEmail(dto.getEmail(), dto.getOtp());
        return ResponseEntity.ok(Map.of("message", "Email verified. Your request is now with the bank for review."));
    }
}