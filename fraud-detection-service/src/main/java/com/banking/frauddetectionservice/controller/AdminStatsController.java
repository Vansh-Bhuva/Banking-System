package com.banking.frauddetectionservice.controller;

import com.banking.frauddetectionservice.repository.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stats/fraud")
@RequiredArgsConstructor
public class AdminStatsController {

    private final FraudAlertRepository fraudAlertRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "totalAlerts", fraudAlertRepository.count(),
                "openAlerts", fraudAlertRepository.countByStatus("OPEN"),
                "dismissedAlerts", fraudAlertRepository.countByStatus("DISMISSED"),
                "actionTakenAlerts", fraudAlertRepository.countByStatus("ACTION_TAKEN")
        ));
    }
}