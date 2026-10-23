package com.banking.accountservice.dto;

import com.banking.accountservice.model.AccountStatus;
import com.banking.accountservice.model.AccountType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminAccountResponse {
    private String id;
    private String accountNumber;
    private Long userId;
    private String accountHolderName;
    private String email;
    private String phone;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private BigDecimal dailyTransactionLimit;
    private BigDecimal dailyTransferred;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}