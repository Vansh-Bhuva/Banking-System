package com.banking.accountservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthServiceClient {

    @GetMapping("/api/auth/internal/users/{userId}/kyc")
    Map<String, Object> getKycStatus(@PathVariable("userId") Long userId);
}