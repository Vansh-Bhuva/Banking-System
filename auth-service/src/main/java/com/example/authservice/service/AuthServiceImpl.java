package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.ChangePasswordRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;
import com.example.authservice.entity.User;
import com.example.authservice.enums.Role;
import com.example.authservice.exception.EmailAlreadyExistsException;
import com.example.authservice.exception.InvalidCredentialsException;
import com.example.authservice.repository.UserRepository;
import com.example.authservice.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;



    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (Boolean.TRUE.equals(user.getBlocked()))
            throw new InvalidCredentialsException("Your account is blocked. Please contact the bank.");

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))
            throw new InvalidCredentialsException("Too many failed attempts. Try again later.");

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            if (attempts >= 5) { user.setLockedUntil(now.plusMinutes(15)); attempts = 0; }
            user.setFailedLoginAttempts(attempts);
            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid email or password");
        }
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        if (Boolean.TRUE.equals(user.getPasswordChangeRequired())) {
            if (user.getTempPasswordExpiresAt() != null && user.getTempPasswordExpiresAt().isBefore(now))
                throw new InvalidCredentialsException("Temporary password expired. Please contact the bank.");
            return response(user, jwtService.generatePreAuthToken(user, "PASSWORD_CHANGE"), "PASSWORD_CHANGE");
        }
        return response(user, jwtService.generateToken(user), "FULL");
    }

    @Override
    @Transactional
    public AuthResponse changePassword(String preAuthToken, ChangePasswordRequest req) {
        Claims claims;
        try {
            claims = jwtService.parsePreAuthToken(preAuthToken, "PASSWORD_CHANGE");
        } catch (Exception e) {
            throw new InvalidCredentialsException("Session expired. Please log in again.");
        }
        User user = userRepository.findById(claims.get("userId", Long.class))
                .orElseThrow(() -> new InvalidCredentialsException("Session expired. Please log in again."));

        if (!Boolean.TRUE.equals(user.getPasswordChangeRequired())
                || !passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new InvalidCredentialsException("Invalid password");
        if (passwordEncoder.matches(req.getNewPassword(), user.getPassword()))
            throw new IllegalArgumentException("New password must be different from the temporary password");

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setPasswordChangeRequired(false);
        user.setTempPasswordExpiresAt(null);
        userRepository.save(user);
        return response(user, jwtService.generateToken(user), "FULL");
    }

    private AuthResponse response(User u, String token, String stage) {
        return AuthResponse.builder().token(token).userId(u.getId()).name(u.getName())
                .email(u.getEmail()).role(u.getRole()).stage(stage).build();
    }
}