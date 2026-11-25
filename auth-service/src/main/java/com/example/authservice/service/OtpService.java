package com.example.authservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final StringRedisTemplate redis;
    private final MailService mailService;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public void send(String purpose, String email) {
        String key = "otp:" + purpose + ":" + email;
        Boolean first = redis.opsForValue().setIfAbsent(key + ":cooldown", "1", Duration.ofSeconds(60));
        if (!Boolean.TRUE.equals(first)) return;

        String otp = String.format("%06d", random.nextInt(1_000_000));
        redis.opsForHash().put(key, "hash", encoder.encode(otp));
        redis.opsForHash().put(key, "attempts", "0");
        redis.expire(key, Duration.ofMinutes(5));

        mailService.send(email, "Your verification code",
                "Your OTP is " + otp + ". It expires in 5 minutes. The bank never asks for it by phone or chat.");
    }

    public boolean verify(String purpose, String email, String input) {
        String key = "otp:" + purpose + ":" + email;
        Object hash = redis.opsForHash().get(key, "hash");
        if (hash == null) return false;

        Long attempts = redis.opsForHash().increment(key, "attempts", 1);
        if (attempts != null && attempts > 3) {
            redis.delete(key);
            return false;
        }

        boolean ok = encoder.matches(input, (String) hash);
        if (ok) redis.delete(key);
        return ok;
    }
}