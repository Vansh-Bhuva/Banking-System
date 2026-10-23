package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.dto.AddBeneficiaryRequest;
import com.banking.transactionservice.dto.BeneficiaryResponse;
import com.banking.transactionservice.model.Beneficiary;
import com.banking.transactionservice.model.BeneficiaryStatus;
import com.banking.transactionservice.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaryRepository;
    private final AccountServiceClient accountServiceClient;

    @Value("${banking.beneficiary.cooling-minutes:30}")
    private long coolingMinutes;

    @Value("${banking.beneficiary.enforce-for-transfer:true}")
    private boolean enforceForTransfer;

    @Value("${banking.beneficiary.enforce-threshold:10000}")
    private BigDecimal enforceThreshold;

    // ───────────────────────────── ADD ─────────────────────────────
    @Transactional
    public BeneficiaryResponse addBeneficiary(AddBeneficiaryRequest request) {
        Long userId = currentUserId();

        if (beneficiaryRepository.existsByUserIdAndAccountNumber(
                userId, request.getAccountNumber())) {
            throw new RuntimeException("Beneficiary already exists");
        }

        // Validate receiver account exists (real bank behavior)
        try {
            accountServiceClient.getAccount(request.getAccountNumber());
        } catch (Exception e) {
            throw new RuntimeException("Invalid beneficiary account: "
                    + request.getAccountNumber());
        }

        Beneficiary b = Beneficiary.builder()
                .userId(userId)
                .beneficiaryName(request.getBeneficiaryName())
                .accountNumber(request.getAccountNumber())
                .ifsc(request.getIfsc())
                .nickname(request.getNickname())
                .status(BeneficiaryStatus.PENDING_COOLING)
                .coolingEndsAt(LocalDateTime.now().plusMinutes(coolingMinutes))
                .build();

        Beneficiary saved = beneficiaryRepository.save(b);
        log.info("Beneficiary added: id={} userId={} account={}",
                saved.getId(), userId, saved.getAccountNumber());
        return map(saved);
    }

    // ───────────────────────────── LIST ────────────────────────────
    public List<BeneficiaryResponse> list() {
        return beneficiaryRepository
                .findByUserIdOrderByCreatedAtDesc(currentUserId())
                .stream()
                .map(this::map)
                .toList();
    }

    // ───────────────────────────── DELETE ──────────────────────────
    @Transactional
    public void delete(String beneficiaryId) {
        Beneficiary b = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));

        if (!b.getUserId().equals(currentUserId())) {
            throw new RuntimeException("Not allowed");
        }

        beneficiaryRepository.delete(b);
        log.info("Beneficiary deleted: id={}", beneficiaryId);
    }

    // ─────────────────── TRANSFER-TIME ENFORCEMENT ─────────────────
    /**
     * Called by TransactionService before debiting sender.
     * Rule:
     *  - if enforceForTransfer = false → always pass
     *  - if amount < enforceThreshold → pass
     *  - otherwise receiver MUST be an ACTIVE beneficiary
     */
    public void assertTransferAllowed(Long userId,
                                      String receiverAccountNumber,
                                      BigDecimal amount) {
        if (!enforceForTransfer) return;
        if (amount.compareTo(enforceThreshold) <= 0) return;

        Beneficiary b = beneficiaryRepository
                .findByUserIdAndAccountNumber(userId, receiverAccountNumber)
                .orElseThrow(() -> new RuntimeException(
                        "Receiver is not a registered beneficiary. " +
                        "Please add the beneficiary first."));

        if (b.getStatus() != BeneficiaryStatus.ACTIVE) {
            throw new RuntimeException(
                    "Beneficiary is still in cooling period. Active from: "
                    + b.getCoolingEndsAt());
        }
    }

    // ─────────────── RECORD USE (called on successful transfer) ────
    @Transactional
    public void recordUse(Long userId, String receiverAccountNumber) {
        beneficiaryRepository
                .findByUserIdAndAccountNumber(userId, receiverAccountNumber)
                .ifPresent(b -> {
                    b.setLastUsedAt(LocalDateTime.now());
                    beneficiaryRepository.save(b);
                });
    }

    // ─────────── SCHEDULED: promote cooled beneficiaries ──────────
    @Scheduled(fixedRate = 60_000) // every minute
    @Transactional
    public void activateCooledBeneficiaries() {
        List<Beneficiary> due = beneficiaryRepository
                .findByStatusAndCoolingEndsAtBefore(
                        BeneficiaryStatus.PENDING_COOLING,
                        LocalDateTime.now());

        due.forEach(b -> {
            b.setStatus(BeneficiaryStatus.ACTIVE);
            log.info("Beneficiary activated: id={}", b.getId());
        });
        if (!due.isEmpty()) beneficiaryRepository.saveAll(due);
    }

    // ──────────────────────────── helpers ─────────────────────────
    private Long currentUserId() {
        return Long.parseLong(SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal()
                .toString());
    }

    private BeneficiaryResponse map(Beneficiary b) {
        BeneficiaryResponse r = new BeneficiaryResponse();
        r.setId(b.getId());
        r.setBeneficiaryName(b.getBeneficiaryName());
        r.setAccountNumber(b.getAccountNumber());
        r.setIfsc(b.getIfsc());
        r.setNickname(b.getNickname());
        r.setStatus(b.getStatus());
        r.setCoolingEndsAt(b.getCoolingEndsAt());
        r.setCreatedAt(b.getCreatedAt());
        r.setLastUsedAt(b.getLastUsedAt());
        return r;
    }
}