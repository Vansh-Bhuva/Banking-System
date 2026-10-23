package com.example.authservice.service;

import com.example.authservice.dto.AdminUserResponse;
import com.example.authservice.entity.User;
import com.example.authservice.enums.KycStatus;
import com.example.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public Map<String, Object> searchUsers(String search, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> result = (search == null || search.isBlank())
                ? userRepository.findAll(p)
                : userRepository
                .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        search, search, p);

        return Map.of(
                "content", result.getContent().stream().map(this::map).toList(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", page
        );
    }

    public AdminUserResponse getUser(Long id) {
        return map(find(id));
    }

    public Map<String, Object> pendingKyc(int page, int size) {
        Pageable p = PageRequest.of(page, size);
        Page<User> result = userRepository.findByKycStatus(KycStatus.PENDING, p);
        return Map.of(
                "content", result.getContent().stream().map(this::map).toList(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", page
        );
    }

    @Transactional
    public AdminUserResponse approveKyc(Long userId, Long actorId, String actorEmail) {
        User u = find(userId);
        String before = "KYC=" + u.getKycStatus();
        u.setKycStatus(KycStatus.FULL_KYC);
        u.setKycVerifiedAt(LocalDateTime.now());
        u.setKycRejectionReason(null);
        userRepository.save(u);

        auditLogService.log(actorId, actorEmail, "APPROVE_KYC",
                "USER", userId.toString(), before, "KYC=FULL_KYC", null);
        return map(u);
    }

    @Transactional
    public AdminUserResponse rejectKyc(Long userId, String reason, Long actorId, String actorEmail) {
        User u = find(userId);
        String before = "KYC=" + u.getKycStatus();
        u.setKycStatus(KycStatus.REJECTED);
        u.setKycRejectionReason(reason);
        u.setKycVerifiedAt(null);
        userRepository.save(u);

        auditLogService.log(actorId, actorEmail, "REJECT_KYC",
                "USER", userId.toString(), before, "KYC=REJECTED reason=" + reason, null);
        return map(u);
    }

    @Transactional
    public AdminUserResponse blockUser(Long userId,Long actorId, String actorEmail) {
        User u = find(userId);
        String before = "blocked=" + u.getBlocked();
        u.setBlocked(true);
        userRepository.save(u);
        auditLogService.log(actorId, actorEmail, "BLOCK_USER",
                "USER", userId.toString(), before, "blocked=true", null);
        return map(u);
    }

    @Transactional
    public AdminUserResponse unblockUser(Long userId,Long actorId, String actorEmail) {
        User u = find(userId);
        String before = "blocked=" + u.getBlocked();
        u.setBlocked(false);
        userRepository.save(u);
        auditLogService.log(actorId, actorEmail, "UNBLOCK_USER",
                "USER", userId.toString(), before, "blocked=false", null);
        return map(u);
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    private AdminUserResponse map(User u) {
        AdminUserResponse r = new AdminUserResponse();
        r.setId(u.getId());
        r.setName(u.getName());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole());
        r.setKycStatus(u.getKycStatus());
        r.setBlocked(u.getBlocked());
        r.setYtdCredits(u.getYtdCredits());
        r.setKycVerifiedAt(u.getKycVerifiedAt());
        r.setKycRejectionReason(u.getKycRejectionReason());
        return r;
    }
}