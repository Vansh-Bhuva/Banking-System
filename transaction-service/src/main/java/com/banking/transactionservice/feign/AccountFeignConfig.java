package com.banking.transactionservice.feign;

import com.banking.transactionservice.config.SystemTokenProvider;
import feign.RequestInterceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AccountFeignConfig {

    private final ApplicationContext context;

    public AccountFeignConfig(ApplicationContext context) {
        this.context = context;
    }

    @Bean
    public RequestInterceptor authForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String header = attrs.getRequest().getHeader("Authorization");
                if (header != null) {
                    template.header("Authorization", header);
                    return;
                }
            }
            SystemTokenProvider provider =
                    context.getBean(SystemTokenProvider.class);
            template.header("Authorization", "Bearer " + provider.getToken());
        };
    }
}