package com.banking.transactionservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddBeneficiaryRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String beneficiaryName;

    @NotBlank
    @Pattern(regexp = "\\d{12}", message = "Account number must be 12 digits")
    private String accountNumber;

    @Pattern(regexp = "[A-Z]{4}0[A-Z0-9]{6}", message = "Invalid IFSC")
    private String ifsc;

    @Size(max = 50)
    private String nickname;
}