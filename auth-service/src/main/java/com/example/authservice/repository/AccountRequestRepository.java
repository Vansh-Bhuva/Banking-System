package com.example.authservice.repository;

import com.example.authservice.entity.AccountRequest;
import com.example.authservice.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {
    Optional<AccountRequest> findTopByEmailAndStatusOrderBySubmittedAtDesc(String email, RequestStatus status);
    Page<AccountRequest> findByStatusAndEmailVerifiedTrue(RequestStatus status, Pageable pageable);
}