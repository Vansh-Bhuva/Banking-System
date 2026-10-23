package com.banking.frauddetectionservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckResult {

    private boolean fraudDetected;

    private String rule;       // VELOCITY, HIGH_AMOUNT, ROUND_NUMBER, etc.
    private String severity;   // LOW, MEDIUM, HIGH
    private String reason;     // human-readable explanation

    // convenience for existing code that uses isFraudDetected() getter
    public boolean isFraudDetected() {
        return fraudDetected;
    }
}