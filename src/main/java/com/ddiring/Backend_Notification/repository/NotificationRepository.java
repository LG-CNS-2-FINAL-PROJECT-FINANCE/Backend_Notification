package com.ddiring.Backend_Notification.repository;

import com.ddiring.Backend_Notification.Entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    boolean existsByEventId(String eventId);
}
