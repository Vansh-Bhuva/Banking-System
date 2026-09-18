package com.banking.notificationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "auth-service",
        url = "http://localhost:8086"
)
public interface AuthServiceClient {

    @GetMapping("/internal/users/{userId}/email")
    String getUserEmail(@PathVariable("userId") Long userId);
}