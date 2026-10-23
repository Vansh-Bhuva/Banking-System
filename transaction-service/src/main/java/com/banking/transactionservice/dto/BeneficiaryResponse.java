package com.banking.transactionservice.dto;

import com.banking.transactionservice.model.BeneficiaryStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BeneficiaryResponse {
    private String id;
    private String beneficiaryName;
    private String accountNumber;
    private String ifsc;
    private String nickname;
    private BeneficiaryStatus status;
    private LocalDateTime coolingEndsAt;
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
}