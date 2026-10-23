package com.banking.transactionservice.controller;

import com.banking.transactionservice.dto.AddBeneficiaryRequest;
import com.banking.transactionservice.dto.BeneficiaryResponse;
import com.banking.transactionservice.service.BeneficiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/beneficiaries")
@RequiredArgsConstructor
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;

    @PostMapping
    public ResponseEntity<BeneficiaryResponse> add(
            @Valid @RequestBody AddBeneficiaryRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(beneficiaryService.addBeneficiary(request));
    }

    @GetMapping
    public ResponseEntity<List<BeneficiaryResponse>> list() {
        return ResponseEntity.ok(beneficiaryService.list());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        beneficiaryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}