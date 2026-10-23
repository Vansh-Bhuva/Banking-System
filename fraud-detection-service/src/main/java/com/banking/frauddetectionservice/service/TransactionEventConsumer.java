package com.banking.frauddetectionservice.service;

import com.banking.frauddetectionservice.model.FraudAlert;
import com.banking.frauddetectionservice.model.FraudCheckResult;
import com.banking.frauddetectionservice.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final FraudDetectionService fraudDetectionService;
    private final FraudAlertRepository fraudAlertRepository;

    @KafkaListener(
            topics = "transaction.initiated",
            groupId = "fraud-detection-group"
    )
    public void consumeTransactionInitiated(@Payload Map<String, Object> payload) {
        Object txIdObj = payload.get("transactionId");
        Object senderObj = payload.get("senderAccountNumber");

        if (txIdObj == null) {
            log.error("Received transaction.initiated without transactionId: {}", payload);
            return;
        }

        String transactionId = txIdObj.toString();
        String senderAccountNumber = senderObj != null ? senderObj.toString() : null;

        log.info("Received transaction for fraud check: {}", transactionId);

        try {
            FraudCheckResult result = fraudDetectionService.checkTransaction(payload);

            if (result != null && result.isFraudDetected()) {
                FraudAlert alert = FraudAlert.builder()
                        .transactionId(transactionId)
                        .accountNumber(senderAccountNumber)
                        .rule(result.getRule() != null ? result.getRule() : "SUSPICIOUS")
                        .severity(result.getSeverity() != null ? result.getSeverity() : "HIGH")
                        .status("OPEN")
                        .build();

                fraudAlertRepository.save(alert);
                log.warn("Fraud alert persisted: tx={} account={} rule={}",
                        transactionId, senderAccountNumber, alert.getRule());
            }
        } catch (Exception e) {
            log.error("Error in fraud detection for tx {}: {}",
                    transactionId, e.getMessage(), e);
        }
    }
}