package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.model.FraudAlert;
import com.banking.frauddetectionservice.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFraudService {

    private final FraudAlertRepository repo;

    public Map<String, Object> list(String status, String severity, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FraudAlert> result;

        if (status != null && severity != null)
            result = repo.findByStatusAndSeverity(status, severity, p);
        else if (status != null)
            result = repo.findByStatus(status, p);
        else if (severity != null)
            result = repo.findBySeverity(severity, p);
        else
            result = repo.findAll(p);

        return Map.of(
                "content", result.getContent(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", page
        );
    }

    public FraudAlert dismiss(String id, String adminEmail, String notes) {
        FraudAlert a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        a.setStatus("DISMISSED");
        a.setReviewedBy(adminEmail);
        a.setReviewedAt(LocalDateTime.now());
        a.setNotes(notes);
        log.info("Alert {} dismissed by {}", id, adminEmail);
        return repo.save(a);
    }

    public FraudAlert markAction(String id, String adminEmail, String notes) {
        FraudAlert a = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        a.setStatus("ACTION_TAKEN");
        a.setReviewedBy(adminEmail);
        a.setReviewedAt(LocalDateTime.now());
        a.setNotes(notes);
        log.info("Alert {} marked ACTION_TAKEN by {}", id, adminEmail);
        return repo.save(a);
    }
}