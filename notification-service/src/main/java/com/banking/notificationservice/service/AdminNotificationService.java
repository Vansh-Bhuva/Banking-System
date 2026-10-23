package com.banking.notificationservice.service;

import com.banking.notificationservice.client.AuthServiceClient;
import com.banking.notificationservice.dto.BroadcastRequest;
import com.banking.notificationservice.model.Notification;
import com.banking.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final NotificationRepository notificationRepository;

    public Map<String, Object> history(Long userId, int page, int size) {
        Pageable p = PageRequest.of(page, size);
        Page<Notification> result = (userId != null)
                ? notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, p)
                : notificationRepository.findAllByOrderByCreatedAtDesc(p);

        return Map.of(
                "content", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", page
        );
    }

    public int broadcast(BroadcastRequest req) {
        List<Long> targets = req.getUserIds();
        if (targets == null || targets.isEmpty()) {
            log.warn("Broadcast with empty userIds — skipping DB insert per-user");
            return 0;
        }

        int count = 0;
        for (Long userId : targets) {
            Notification n = new Notification();
            n.setUserId(userId);
            n.setSubject(req.getTitle());
            n.setMessage(req.getMessage());
            n.setType("BROADCAST");
            n.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(n);
            count++;
        }
        log.info("Broadcast sent to {} users: {}", count, req.getTitle());
        return count;
    }
}