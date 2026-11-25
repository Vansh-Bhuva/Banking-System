package com.example.authservice.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data

public class AccountRequestDto {
    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "Phone must be 10 digits")
    private String phone;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    private String address;

    @NotBlank
    @Pattern(regexp = "\\d{12}", message = "Aadhaar must be 12 digits")
    private String aadhaar;

    @NotBlank
    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]", message = "Invalid PAN")
    private String pan;
}
