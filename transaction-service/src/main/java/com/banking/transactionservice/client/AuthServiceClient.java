package com.banking.transactionservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "auth-service",
        url = "${auth.service.url}")
public interface AuthServiceClient {

    @PostMapping("/api/auth/internal/service-token")
    Map<String, String> issueServiceToken(
            @RequestHeader("X-Internal-Key") String internalKey);
}