package com.banking.notificationservice.controller;

import com.banking.notificationservice.model.Notification;
import com.banking.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/{accountNumber}")
    public List<Notification> getNotifications(
            @PathVariable String accountNumber
    ) {
        return notificationRepository
                .findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    @GetMapping("/{accountNumber}/unread-count")
    public long getUnreadCount(
            @PathVariable String accountNumber
    ) {
        return notificationRepository
                .countByAccountNumberAndReadFalse(accountNumber);
    }
}