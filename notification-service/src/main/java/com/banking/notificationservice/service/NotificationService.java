package com.banking.notificationservice.service;

import com.banking.notificationservice.client.AuthServiceClient;
import com.banking.notificationservice.model.Notification;
import com.banking.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthServiceClient authServiceClient;
    private final JavaMailSender mailSender;

    /**
     * Transaction completed — debit sender, credit receiver.
     */
    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(
            @Payload Map<String, Object> payload) {
        try {
            Long userId = payload.get("userId") != null
                    ? ((Number) payload.get("userId")).longValue()
                    : null;
            String senderAccount = (String) payload
                    .get("senderAccountNumber");
            String receiverAccount = (String) payload
                    .get("receiverAccountNumber");
            String amount = payload.get("amount").toString();
            String transactionId = payload.get("transactionId").toString();

            saveNotification(
                    userId,
                    senderAccount,
                    "TRANSACTION_COMPLETED",
                    "DEBIT ALERT",
                    String.format("₹%s debited from account %s", amount, senderAccount),
                    transactionId
            );

            sendAlert(senderAccount, "DEBIT ALERT",
                    String.format("₹%s debited from account %s",
                            amount, senderAccount));

            sendAlert(receiverAccount, "CREDIT ALERT",
                    String.format("₹%s credited to account %s",
                            amount, receiverAccount));

        } catch (Exception e) {
            log.error("Error sending transaction notification: {}",
                    e.getMessage());
        }
    }


    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            sendAlert(accountNumber,
                    "🚨 ACCOUNT BLOCKED",
                    String.format(
                            "Your account %s has been blocked. " +
                                    "Reason: %s. " +
                                    "Please contact your bank immediately.",
                            accountNumber, reason));

        } catch (Exception e) {
            log.error("Error sending fraud alert: {}", e.getMessage());
        }
    }


    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(
            @Payload Map<String, Object> payload) {
        try {
            Long userId = (Long) payload.get("userId");
            String accountNumber = (String) payload.get("accountNumber");
            String otp = (String) payload.get("otp");
            String transactionId = (String) payload.get("transactionId");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            String email = authServiceClient.getUserEmail(userId);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Transaction OTP Verification");
            message.setText(
                    "Your OTP is: " + otp + "\n\n" +
                            "Transaction ID: " + transactionId + "\n" +
                            "Amount: ₹" + amount + "\n\n" +
                            "This OTP is valid for 5 minutes."
            );

            mailSender.send(message);

            saveNotification(
                    userId,
                    accountNumber,
                    "OTP_REQUIRED",
                    "Transaction Verification Required",
                    String.format(
                            "Suspicious activity detected. Reason: %s. " +
                                    "Transaction of ₹%s requires OTP verification.",
                            reason,
                            amount
                    ),
                    transactionId
            );

            sendAlert(accountNumber,
                    "🔐 TRANSACTION VERIFICATION REQUIRED",
                    String.format(
                            "Suspicious activity detected on your account. " +
                                    "Reason: %s. " +
                                    "A transaction of ₹%s is pending verification. " +
                                    "Your OTP is: %s. Valid for 5 minutes. " +
                                    "If this wasn't you — ignore this message. " +
                                    "Transaction will be cancelled and amount refunded automatically.",
                            reason, amount, otp, transactionId, otp));

        } catch (Exception e) {
            log.error("Error sending OTP notification: {}",
                    e.getMessage());
        }
    }


    @KafkaListener(topics = "transaction.refunded")
    public void consumeTransactionRefunded(
            @Payload Map<String, Object> payload) {
        try {
            String senderAccount = (String) payload
                    .get("senderAccountNumber");
            String amount = payload.get("amount").toString();
            String reason = (String) payload.get("reason");

            sendAlert(senderAccount, "💰 REFUND PROCESSED",
                    String.format(
                            "Your transaction of ₹%s was cancelled. " +
                                    "Reason: %s. " +
                                    "₹%s has been refunded to account %s.",
                            amount, reason, amount, senderAccount));

        } catch (Exception e) {
            log.error("Error sending refund notification: {}",
                    e.getMessage());
        }
    }

    /**
     * Payment completed via Razorpay.
     */
    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();

            sendAlert(accountNumber, "PAYMENT SUCCESSFUL",
                    String.format(
                            "Payment of ₹%s completed. " +
                                    "Razorpay ID: %s",
                            amount, payload.get("razorpayPaymentId")));

        } catch (Exception e) {
            log.error("Error sending payment notification: {}",
                    e.getMessage());
        }
    }

    /**
     * Payment failed via Razorpay.
     */
    @KafkaListener(topics = "payment.failed")
    public void consumePaymentFailed(
            @Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = payload.get("amount").toString();

            sendAlert(accountNumber, "❌ PAYMENT FAILED",
                    String.format(
                            "Your payment of ₹%s could not be processed. " +
                                    "Please try again or contact support.",
                            amount));

        } catch (Exception e) {
            log.error("Error sending payment failure notification: {}",
                    e.getMessage());
        }
    }

    private void sendAlert(String accountNumber,
                           String subject,
                           String message) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("NOTIFICATION SENT");
        log.info("Account : {}", accountNumber);
        log.info("Subject : {}", subject);
        log.info("Message : {}", message);
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void saveNotification(
            Long userId,
            String accountNumber,
            String type,
            String subject,
            String message,
            String transactionId
    ) {
        Notification notification = Notification.builder()
                .userId(userId)
                .accountNumber(accountNumber)
                .type(type)
                .subject(subject)
                .message(message)
                .transactionId(transactionId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);
    }
}
