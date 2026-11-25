package com.example.authservice.dto;

import com.example.authservice.enums.RequestStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AccountRequestView(Long id, String name, String email, String phone,
                                 LocalDate dateOfBirth, String address, String aadhaarMasked, String panMasked,
                                 RequestStatus status, LocalDateTime submittedAt, String rejectionReason) {
}