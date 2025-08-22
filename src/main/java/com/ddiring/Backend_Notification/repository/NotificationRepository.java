package com.ddiring.Backend_Notification.repository;

import com.ddiring.Backend_Notification.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    Optional<Notification> findByMessage(String message);
}