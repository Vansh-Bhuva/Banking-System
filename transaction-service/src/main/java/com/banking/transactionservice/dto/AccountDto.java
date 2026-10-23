package com.banking.transactionservice.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AccountDto {
    private String id;
    private String accountNumber;
    private Long userId;
    private String accountHolderName;
    private String status;
    private BigDecimal balance;
}