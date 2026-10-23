package com.banking.transactionservice.service;

import com.banking.transactionservice.model.AuditLog;
import com.banking.transactionservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository repo;

    public void log(Long actorUserId, String actorEmail, String action,
                    String targetType, String targetId,
                    String before, String after, String ip) {
        repo.save(AuditLog.builder()
                .actorUserId(actorUserId)
                .actorEmail(actorEmail)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .beforeState(before)
                .afterState(after)
                .ipAddress(ip)
                .build());
    }

    public Map<String, Object> search(Long actor, String action, int page, int size) {
        Pageable p = PageRequest.of(page, size);
        Page<AuditLog> result;
        if (actor != null) result = repo.findByActorUserIdOrderByCreatedAtDesc(actor, p);
        else if (action != null) result = repo.findByActionOrderByCreatedAtDesc(action, p);
        else result = repo.findAllByOrderByCreatedAtDesc(p);

        return Map.of(
                "content", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", page
        );
    }
}