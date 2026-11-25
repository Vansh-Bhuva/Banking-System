package com.example.authservice.service;

import com.example.authservice.dto.AccountRequestDto;
import com.example.authservice.dto.AccountRequestView;
import com.example.authservice.entity.AccountRequest;
import com.example.authservice.entity.User;
import com.example.authservice.enums.KycStatus;
import com.example.authservice.enums.RequestStatus;
import com.example.authservice.enums.Role;
import com.example.authservice.repository.AccountRequestRepository;
import com.example.authservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AccountRequestService {

    private final AccountRequestRepository repo;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final MailService mailService;
    private final PasswordEncoder encoder;
    private final AuditLogService auditLogService;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Public: apply ────────────────────────────────
    public void submit(AccountRequestDto dto) {
        String email = dto.getEmail().trim().toLowerCase();

        if (Period.between(dto.getDateOfBirth(), LocalDate.now()).getYears() < 18)
            throw new IllegalArgumentException("Applicant must be at least 18 years old");

        // Same response in every case, so emails can't be enumerated
        if (userRepository.existsByEmail(email)) return;

        var pending = repo.findTopByEmailAndStatusOrderBySubmittedAtDesc(email, RequestStatus.PENDING);
        if (pending.isPresent()) {
            if (!pending.get().isEmailVerified()) otpService.send("ACCOUNT_REQUEST", email);
            return;
        }

        AccountRequest r = new AccountRequest();
        r.setName(dto.getName().trim());
        r.setEmail(email);
        r.setPhone(dto.getPhone());
        r.setDateOfBirth(dto.getDateOfBirth());
        r.setAddress(dto.getAddress());
        r.setAadhaar(dto.getAadhaar());
        r.setPan(dto.getPan());
        repo.save(r);

        otpService.send("ACCOUNT_REQUEST", email);
    }

    public void verifyEmail(String email, String otp) {
        String e = email.trim().toLowerCase();
        AccountRequest r = repo.findTopByEmailAndStatusOrderBySubmittedAtDesc(e, RequestStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));
        if (!otpService.verify("ACCOUNT_REQUEST", e, otp))
            throw new IllegalArgumentException("Invalid or expired code");
        r.setEmailVerified(true);
        repo.save(r);
    }

    // ── Admin ────────────────────────────────────────
    public Map<String, Object> list(RequestStatus status, int page, int size) {
        Page<AccountRequest> result = repo.findByStatusAndEmailVerifiedTrue(
                status, PageRequest.of(page, size, Sort.by("submittedAt").descending()));
        return Map.of(
                "content", result.getContent().stream().map(this::view).toList(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "page", page);
    }

    @Transactional
    public void approve(Long id, Long adminId, String adminEmail) {
        AccountRequest r = find(id);
        if (r.getStatus() != RequestStatus.PENDING || !r.isEmailVerified())
            throw new IllegalStateException("Request cannot be approved");
        if (userRepository.existsByEmail(r.getEmail()))
            throw new IllegalStateException("A user with this email already exists");

        String tempPassword = generateTempPassword();
        LocalDateTime now = LocalDateTime.now();

        User u = User.builder()
                .name(r.getName())
                .email(r.getEmail())
                .phone(r.getPhone())
                .password(encoder.encode(tempPassword))
                .role(Role.CUSTOMER)
                .aadhaarEncrypted(r.getAadhaar())
                .panEncrypted(r.getPan())
                .kycStatus(KycStatus.FULL_KYC)
                .kycSubmittedAt(r.getSubmittedAt())
                .kycVerifiedAt(now)
                .passwordChangeRequired(true)
                .tempPasswordExpiresAt(now.plusHours(24))
                .build();
        userRepository.save(u);

        r.setStatus(RequestStatus.APPROVED);
        r.setReviewedAt(now);
        r.setReviewedBy(adminId);
        repo.save(r);

        auditLogService.log(adminId, adminEmail, "APPROVE_ACCOUNT_REQUEST", "ACCOUNT_REQUEST",
                id.toString(), "status=PENDING", "status=APPROVED userId=" + u.getId(), null);

        // Send credentials only after the DB commit succeeds
        String email = u.getEmail(), name = u.getName();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mailService.send(email, "Your bank account request is approved",
                        "Hello " + name + ",\n\nYour request was approved.\n\n"
                                + "Login email: " + email + "\nTemporary password: " + tempPassword + "\n\n"
                                + "It expires in 24 hours, and you must change it at first login.");
            }
        });
    }

    @Transactional
    public void reject(Long id, String reason, Long adminId, String adminEmail) {
        AccountRequest r = find(id);
        if (r.getStatus() != RequestStatus.PENDING)
            throw new IllegalStateException("Request already processed");
        r.setStatus(RequestStatus.REJECTED);
        r.setRejectionReason(reason);
        r.setReviewedAt(LocalDateTime.now());
        r.setReviewedBy(adminId);
        repo.save(r);
        auditLogService.log(adminId, adminEmail, "REJECT_ACCOUNT_REQUEST", "ACCOUNT_REQUEST",
                id.toString(), "status=PENDING", "status=REJECTED reason=" + reason, null);

        String email = r.getEmail();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                mailService.send(email, "Your bank account request",
                        "We could not approve your request.\nReason: " + reason);
            }
        });
    }

    // ── helpers ──────────────────────────────────────
    private AccountRequest find(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
    }

    private AccountRequestView view(AccountRequest r) {
        String a = r.getAadhaar(), p = r.getPan();
        return new AccountRequestView(r.getId(), r.getName(), r.getEmail(), r.getPhone(),
                r.getDateOfBirth(), r.getAddress(),
                a == null ? null : "XXXXXXXX" + a.substring(a.length() - 4),
                p == null ? null : "XXXXX" + p.substring(5, 9) + "X",
                r.getStatus(), r.getSubmittedAt(), r.getRejectionReason());
    }

    private String generateTempPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ", lower = "abcdefghijkmnopqrstuvwxyz",
                digits = "23456789", special = "@#$%&*", all = upper + lower + digits + special;
        List<Character> c = new ArrayList<>();
        c.add(pick(upper));
        c.add(pick(lower));
        c.add(pick(digits));
        c.add(pick(special));
        for (int i = 0; i < 8; i++) c.add(pick(all));
        Collections.shuffle(c, RANDOM);
        StringBuilder sb = new StringBuilder();
        c.forEach(sb::append);
        return sb.toString();
    }

    private char pick(String s) {
        return s.charAt(RANDOM.nextInt(s.length()));
    }
}