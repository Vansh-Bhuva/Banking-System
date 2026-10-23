package com.banking.notificationservice.repository;

import com.banking.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, String> {

    List<Notification> findByAccountNumberOrderByCreatedAtDesc(
            String accountNumber
    );

    long countByAccountNumberAndReadFalse(String accountNumber);

    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}