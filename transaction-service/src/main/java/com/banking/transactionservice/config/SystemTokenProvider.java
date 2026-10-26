package com.banking.transactionservice.config;

import com.banking.transactionservice.client.AuthServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class SystemTokenProvider {

    private final AuthServiceClient authServiceClient;

    @Value("${internal.key}")
    private String internalKey;

    private String cachedToken;
    private Instant cachedExpiry = Instant.EPOCH;

    public synchronized String getToken() {
        if (cachedToken != null && Instant.now().isBefore(cachedExpiry)) {
            return cachedToken;
        }
        try {
            log.info("Fetching service token with key: [{}]", internalKey);
            String token = authServiceClient
                    .issueServiceToken(internalKey)
                    .get("token");
            log.info("Token :: {}", token);
            cachedToken = token;
            cachedExpiry = Instant.now().plusSeconds(8 * 60);
            log.info("Fetched fresh service token");
            return token;
        } catch (Exception e) {
            log.error("Failed to fetch service token: {}", e.getMessage());
            throw new RuntimeException("Service token unavailable", e);
        }
    }
}