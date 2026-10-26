package com.example.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class KycSubmitRequest {
    @NotBlank
    @Pattern(regexp = "\\d{12}", message = "Aadhaar must be 12 digits")
    private String aadhaar;

    @NotBlank
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]", message = "Invalid PAN")
    private String pan;
}