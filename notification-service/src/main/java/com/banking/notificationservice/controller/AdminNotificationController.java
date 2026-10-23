package com.banking.notificationservice.controller;

import com.banking.notificationservice.dto.BroadcastRequest;
import com.banking.notificationservice.service.AdminNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> history(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.history(userId, page, size));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Map<String, Object>> broadcast(
            @Valid @RequestBody BroadcastRequest req) {
        int sent = service.broadcast(req);
        return ResponseEntity.ok(Map.of("sent", sent));
    }
}