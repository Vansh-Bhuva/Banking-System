package com.example.authservice.service;

import com.example.authservice.dto.AuthResponse;
import com.example.authservice.dto.ChangePasswordRequest;
import com.example.authservice.dto.LoginRequest;
import com.example.authservice.dto.RegisterRequest;

public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse changePassword(String preAuthToken, ChangePasswordRequest request);
}