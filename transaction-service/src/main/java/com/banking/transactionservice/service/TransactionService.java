package com.banking.transactionservice.service;

import com.banking.transactionservice.client.AccountServiceClient;
import com.banking.transactionservice.dto.TransactionResponse;
import com.banking.transactionservice.dto.TransferRequest;
import com.banking.transactionservice.event.TransactionCompletedEvent;
import com.banking.transactionservice.event.TransactionInitiatedEvent;
import com.banking.transactionservice.model.Transaction;
import com.banking.transactionservice.model.TransactionStatus;
import com.banking.transactionservice.model.TransactionType;
import com.banking.transactionservice.repository.TransactionRepository;
import com.banking.transactionservice.service.BeneficiaryService;
import com.banking.transactionservice.dto.AccountDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountServiceClient accountServiceClient;
    private final BeneficiaryService beneficiaryService;

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TRANSACTION_INITIATED_TOPIC = "transaction.initiated";
    private static final String TRANSACTION_COMPLETED_TOPIC = "transaction.completed";
    private static final String TRANSACTION_REFUNDED_TOPIC = "transaction.refunded";
    private static final String FRAUD_DETECTED_TOPIC = "fraud.detected";

    public TransactionResponse transfer(TransferRequest request) {

        String principal = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();

        Long userId = Long.parseLong(principal);

        // ── SECURITY: verify sender account belongs to the logged-in user ──
        AccountDto sender;
        try {
            sender = accountServiceClient.getAccount(request.getSenderAccountNumber());
        } catch (Exception e) {
            throw new RuntimeException("Sender account not found");
        }

        if (sender.getUserId() == null || !sender.getUserId().equals(userId)) {
            log.warn("User {} attempted transfer from unauthorized account {}",
                    userId, request.getSenderAccountNumber());
            throw new RuntimeException(
                    "You are not authorized to transfer from this account");
        }

        if (!"ACTIVE".equals(sender.getStatus())) {
            throw new RuntimeException(
                    "Sender account is not active. Status: " + sender.getStatus());
        }

        if (request.getSenderAccountNumber().equals(request.getReceiverAccountNumber())) {
            throw new RuntimeException("Sender and receiver cannot be the same account");
        }

        // Enforce beneficiary rule (only for registered beneficiaries above threshold)
        beneficiaryService.assertTransferAllowed(
                userId,
                request.getReceiverAccountNumber(),
                request.getAmount());

        beneficiaryService.recordUse(userId, request.getReceiverAccountNumber());

        log.info("SAGA START - Transfer: {} -> {} amount: {}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount());

        // SAGA STEP 1: Deduct from sender
        accountServiceClient.deductBalance(
                request.getSenderAccountNumber(),
                request.getAmount());

        Transaction transaction = new Transaction();

        transaction.setUserId(userId);
        transaction.setSenderAccountNumber(request.getSenderAccountNumber());
        transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setDescription(request.getDescription());
        transaction.setReferenceNumber(UUID.randomUUID().toString());

        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction saved as PROCESSING: {}", savedTransaction.getId());

        // SAGA STEP - 2: Publish for fraud check
        TransactionInitiatedEvent event = new TransactionInitiatedEvent(
                savedTransaction.getId(),
                savedTransaction.getSenderAccountNumber(),
                savedTransaction.getReceiverAccountNumber(),
                savedTransaction.getAmount(),
                savedTransaction.getDescription()
        );

        kafkaTemplate.send(TRANSACTION_INITIATED_TOPIC, savedTransaction.getId(), event);
        log.info("SAGA STEP 2 - TransactionInitiatedEvent published: {}", savedTransaction.getId());

        return mapToResponse(savedTransaction);
    }

    public TransactionResponse getTransaction(String transactionId) {
        return mapToResponse(transactionRepository
                .findById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found: " + transactionId
                )));
    }

    public List<TransactionResponse> getTransactionHistory(String accountNumber) {
        // Ownership check
        Long currentUserId = Long.parseLong(
                SecurityContextHolder.getContext()
                        .getAuthentication().getPrincipal().toString());

        AccountDto account = accountServiceClient.getAccount(accountNumber);
        if (account.getUserId() == null || !account.getUserId().equals(currentUserId)) {
            throw new RuntimeException("Not authorized to view this account's history");
        }

        return transactionRepository
                .findAllByAccount(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TransactionResponse verifyOTP(String transactionID, String otp) {
        log.info("OTP verification for the transaction: {}", transactionID);

        Transaction transaction = transactionRepository.findById(transactionID)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found " + transactionID
                ));

        String otpKey = "verification:otp" + transactionID;
        String storedOtp = redisTemplate.opsForValue().get(otpKey);

        if (storedOtp == null) {
            // OTP EXPIRED
            log.warn("OTP expired for transaction: {}", transactionID);
            compensateTransaction(transaction, "OTP expired - transaction cancelled and amount refunded");
            return mapToResponse(transaction);
        }

        if (!storedOtp.equals(otp)) {
            // BLOCK ACCOUNT AND REFUND
            log.warn("Wrong OTP - blocking account and refunding: {}", transactionID);
            redisTemplate.delete(otpKey);
            blockAccountAndCompensate(transaction,
                    "Wrong OTP entered - transaction cancelled, " +
                            "account blocked for security");

            return mapToResponse(transaction);
        }

        // OTP correct - complete transaction
        log.info("OTP verified - completing transaction: {}", transactionID);
        redisTemplate.delete(otpKey);
        completeTransaction(transaction);
        return mapToResponse(transaction);
    }

    private void compensateTransaction(Transaction transaction, String reason) {
        log.warn("SAGA COMPENSATION - refunding: {} amount: {}",
                transaction.getSenderAccountNumber(),
                transaction.getAmount());

        // CREDIT MONEY BACK TO SENDER SYNCHRONOUSLY
        accountServiceClient.creditBalance(
                transaction.getSenderAccountNumber(),
                transaction.getAmount());

        transaction.setStatus(TransactionStatus.FLAGGED);
        transaction.setFailureReason(reason +
                " - SAGA Compensation executed, amount refunded at " + LocalDateTime.now());

        transactionRepository.save(transaction);

        // PUBLISH refund event - Notification service will alert user
        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("transactionId", transaction.getId());
        refundEvent.put("senderAccountNumber", transaction.getSenderAccountNumber());
        refundEvent.put("amount", transaction.getAmount());
        refundEvent.put("reason", reason);

        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, transaction.getId(), refundEvent);

        log.info("SAGA COMPENSATION COMPLETE - {} refunded to  {}",
                transaction.getAmount(), transaction.getSenderAccountNumber());
    }

    private void blockAccountAndCompensate(Transaction transaction, String reason) {

        // Publish fraud.detected -> Account Service will block account
        Map<String, Object> fraudEvent = new HashMap<>();
        fraudEvent.put("transactionId", transaction.getId());
        fraudEvent.put("accountNumber", transaction.getSenderAccountNumber());
        fraudEvent.put("reason", reason);

        kafkaTemplate.send(FRAUD_DETECTED_TOPIC, transaction.getSenderAccountNumber(), fraudEvent);
        log.warn("fraud.detected published - account: {} will be blocked, Kindly contact to the bank",
                transaction.getSenderAccountNumber());

        // SAGA COMPENSATION - refund Sender
        compensateTransaction(transaction, reason);
    }

    private void completeTransaction(Transaction transaction) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        TransactionCompletedEvent completedEvent = new TransactionCompletedEvent(
                transaction.getId(),
                transaction.getSenderAccountNumber(),
                transaction.getReceiverAccountNumber(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getUserId()
        );

        kafkaTemplate.send(TRANSACTION_COMPLETED_TOPIC, transaction.getId(), completedEvent);

        log.info("SAGA COMPLETE - Transaction {} completed",
                transaction.getId());
    }

    public void processCleanResult(String transactionID) {
        Optional<Transaction> opt = transactionRepository.findById(transactionID);

        if (opt.isEmpty()) {
            log.warn("Clean result for unknown tx {} — ignoring (stale/truncated)", transactionID);
            return;
        }

        Transaction transaction = opt.get();

        if (transaction.getStatus() != TransactionStatus.PROCESSING) {
            log.warn("Transaction {} not PROCESSING (status={}) — skipping",
                    transactionID, transaction.getStatus());
            return;
        }

        completeTransaction(transaction);
    }

    public Map<String, Object> getOTP(String transactionID) {
        String otpKey = "verification:otp" + transactionID;   // ← must match generation key
        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) throw new RuntimeException("OTP expired or not generated");
        return Map.of("otp", storedOtp, "otpRequired", true);
    }

    @Scheduled(fixedRate = 60_000)   // every 60 seconds
    @Transactional
    public void expireStaleOtpTransfers() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        List<Transaction> stale = transactionRepository
                .findByStatusAndCreatedAtBefore(TransactionStatus.PROCESSING, cutoff);

        if (stale.isEmpty()) return;

        log.warn("Found {} stale PROCESSING transactions — refunding", stale.size());

        for (Transaction tx : stale) {
            try {
                compensateTransaction(tx, "OTP not verified within time limit — auto-refunded");
                log.info("Auto-refunded tx {} amount {} to {}",
                        tx.getId(), tx.getAmount(), tx.getSenderAccountNumber());
            } catch (Exception e) {
                log.error("Auto-refund failed for tx {}: {}", tx.getId(), e.getMessage());
            }
        }
    }

    private TransactionResponse mapToResponse(Transaction transaction) {

        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setSenderAccountNumber(
                transaction.getSenderAccountNumber());
        response.setReceiverAccountNumber(
                transaction.getReceiverAccountNumber());
        response.setAmount(transaction.getAmount());
        response.setType(transaction.getType());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setReferenceNumber(transaction.getReferenceNumber());
        response.setFailureReason(transaction.getFailureReason());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCompletedAt(transaction.getCompletedAt());

        return response;
    }

    @Transactional
    public void adminReverse(Transaction transaction, String reason) {
        try {
            accountServiceClient.deductBalance(
                    transaction.getReceiverAccountNumber(),
                    transaction.getAmount());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot reverse: receiver has insufficient balance or inactive account. "
                            + e.getMessage());
        }

        try {
            accountServiceClient.creditBalance(
                    transaction.getSenderAccountNumber(),
                    transaction.getAmount());
        } catch (Exception e) {
            // rollback — credit receiver back
            try {
                accountServiceClient.creditBalance(
                        transaction.getReceiverAccountNumber(),
                        transaction.getAmount());
            } catch (Exception rollbackErr) {
                log.error("Rollback failed for tx {}", transaction.getId(), rollbackErr);
            }
            throw new IllegalStateException("Cannot credit sender: " + e.getMessage());
        }

        transaction.setStatus(TransactionStatus.REVERSED);
        transaction.setFailureReason("Admin reversal: " + reason);
        transactionRepository.save(transaction);

        Map<String, Object> refundEvent = new HashMap<>();
        refundEvent.put("transactionId", transaction.getId());
        refundEvent.put("senderAccountNumber", transaction.getSenderAccountNumber());
        refundEvent.put("amount", transaction.getAmount());
        refundEvent.put("reason", "Admin reversal: " + reason);
        kafkaTemplate.send(TRANSACTION_REFUNDED_TOPIC, transaction.getId(), refundEvent);

        log.info("Admin reversal complete for tx {}", transaction.getId());
    }
}
