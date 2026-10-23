package com.banking.transactionservice.controller;

import com.banking.transactionservice.dto.BeneficiaryResponse;
import com.banking.transactionservice.model.Beneficiary;
import com.banking.transactionservice.model.BeneficiaryStatus;
import com.banking.transactionservice.repository.BeneficiaryRepository;
import com.banking.transactionservice.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/beneficiaries")
@RequiredArgsConstructor
public class AdminBeneficiaryController {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AuditLogService auditLogService;

    private Long currentActor() {
        return (Long) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    @GetMapping
    public ResponseEntity<List<Beneficiary>> byUser(@RequestParam Long userId) {
        return ResponseEntity.ok(
                beneficiaryRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<Beneficiary> activate(@PathVariable String id) {
        Beneficiary b = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        String before = "status=" + b.getStatus();
        b.setStatus(BeneficiaryStatus.ACTIVE);
        b.setCoolingEndsAt(LocalDateTime.now());
        beneficiaryRepository.save(b);
        auditLogService.log(currentActor(), "admin", "ACTIVATE_BENEFICIARY",
                "BENEFICIARY", id, before, "status=ACTIVE", null);
        return ResponseEntity.ok(b);
    }

    @PutMapping("/{id}/block")
    public ResponseEntity<Beneficiary> block(@PathVariable String id) {
        Beneficiary b = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        String before = "status=" + b.getStatus();
        b.setStatus(BeneficiaryStatus.BLOCKED);
        beneficiaryRepository.save(b);
        auditLogService.log(currentActor(), "admin", "BLOCK_BENEFICIARY",
                "BENEFICIARY", id, before, "status=BLOCKED", null);
        return ResponseEntity.ok(b);
    }
}