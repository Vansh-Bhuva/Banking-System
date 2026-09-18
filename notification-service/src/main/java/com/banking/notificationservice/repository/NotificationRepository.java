package com.banking.notificationservice.repository;

import com.banking.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, String> {

    List<Notification> findByAccountNumberOrderByCreatedAtDesc(
            String accountNumber
    );

    long countByAccountNumberAndReadFalse(String accountNumber);
}