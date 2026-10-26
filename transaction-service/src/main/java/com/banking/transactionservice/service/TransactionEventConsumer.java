package com.banking.transactionservice.service;


import com.banking.transactionservice.model.Transaction;
import com.banking.transactionservice.model.TransactionStatus;
import com.banking.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final TransactionRepository transactionRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionService transactionService;
    private static final long OTP_EXPIRY_MINUTES = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TRANSACTION_OTP_GENERATED_TOPIC = "transaction.otp.generated";

    @KafkaListener(topics = "verification.required", groupId = "transaction-service")
    public void consumeVerificationRequired(@Payload Map<String, Object> payload) {
        Object txIdObj = payload.get("transactionId");
        if (txIdObj == null) return;

        String txId = txIdObj.toString();
        String accountNumber = (String) payload.get("accountNumber");
        Object amount = payload.get("amount");
        String reason = (String) payload.get("reason");

        log.warn("Verification required for tx {} — reason: {}", txId, reason);

        try {
            // Get the transaction to find userId
            Transaction tx = transactionRepository.findById(txId).orElse(null);
            if (tx == null) {
                log.warn("Verification event for unknown tx {} — ignoring", txId);
                return;
            }

            // Generate OTP
            String otp = String.format("%06d",
                    new java.security.SecureRandom().nextInt(1_000_000));

            // Store in Redis with 5-min TTL
            redisTemplate.opsForValue().set(
                    "verification:otp" + txId,
                    otp,
                    5, java.util.concurrent.TimeUnit.MINUTES);

            log.info("OTP generated for tx {} (dev: {})", txId, otp);

            // Publish to notification-service WITH userId + otp
            Map<String, Object> otpEvent = new HashMap<>();
            otpEvent.put("userId", tx.getUserId());
            otpEvent.put("accountNumber", accountNumber);
            otpEvent.put("otp", otp);
            otpEvent.put("transactionId", txId);
            otpEvent.put("amount", amount);
            otpEvent.put("reason", reason);

            kafkaTemplate.send("transaction.otp.generated", txId, otpEvent);
            log.info("OTP event published to transaction.otp.generated for tx {}", txId);
        } catch (Exception e) {
            log.error("Failed to generate OTP for tx {}: {}", txId, e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "fraud.check.clean")
    public void consumeFraudCheckCleanResult(
            @Payload Map<String, Object> payload){

        try{

            String transactionId =  (String) payload.get("transactionId");
            transactionService.processCleanResult(transactionId);
        }
        catch(Exception e){
            log.error("Error processing fraud check result: {}", e.getMessage());
        }
    }


    @KafkaListener(topics = "fraud.check.clean", groupId = "transaction-service")
    public void consumeFraudClean(@Payload Map<String, Object> payload) {
        Object txIdObj = payload.get("transactionId");
        if (txIdObj == null) {
            log.warn("fraud.check.clean payload missing transactionId: {}", payload);
            return;
        }

        String txId = txIdObj.toString();
        log.info("Fraud check clean received for tx {}", txId);

        try {
            transactionService.processCleanResult(txId);
        } catch (Exception e) {
            log.error("Failed to complete tx {}: {}", txId, e.getMessage(), e);
        }
    }

}
