package com.example.authservice.config;

import com.example.authservice.entity.User;
import com.example.authservice.enums.KycStatus;
import com.example.authservice.enums.Role;
import com.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "admin@bank.local";

        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists: {}", adminEmail);
            return;
        }

        User admin = User.builder()
                .name("Bank Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode("Admin@123"))
                .role(Role.ADMIN)
                .kycStatus(KycStatus.FULL_KYC)
                .ytdCredits(BigDecimal.ZERO)
                .build();

        userRepository.save(admin);
        log.info("=====================================================");
        log.info("Admin user seeded");
        log.info("Email:    {}", adminEmail);
        log.info("Password: Admin@123");
        log.info("Change this in production!");
        log.info("=====================================================");
    }
}