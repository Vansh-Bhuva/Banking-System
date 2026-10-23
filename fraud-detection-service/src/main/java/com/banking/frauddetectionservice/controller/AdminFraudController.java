package com.banking.frauddetectionservice.controller;

import com.banking.frauddetectionservice.model.FraudAlert;
import com.banking.frauddetectionservice.service.AdminFraudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/fraud/alerts")
@RequiredArgsConstructor
public class AdminFraudController {

    private final AdminFraudService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.list(status, severity, page, size));
    }

    @PutMapping("/{id}/dismiss")
    public ResponseEntity<FraudAlert> dismiss(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "") String notes) {
        return ResponseEntity.ok(service.dismiss(id, currentEmail(), notes));
    }

    @PutMapping("/{id}/action")
    public ResponseEntity<FraudAlert> action(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "") String notes) {
        return ResponseEntity.ok(service.markAction(id, currentEmail(), notes));
    }

    private String currentEmail() {
        // fraud-service JWT principal is Long userId — return as string
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return p != null ? p.toString() : "unknown";
    }
}