package com.banking.transactionservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Transaction Service API",
                version = "1.0",
                description = "APIs for transaction management"
        )
)
public class OpenApiConfig {
}
