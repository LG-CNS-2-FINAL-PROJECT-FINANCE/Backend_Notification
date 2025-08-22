package com.ddiring.Backend_Notification.repository;

import com.ddiring.Backend_Notification.Entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {
    @Query("SELECT un FROM UserNotification un JOIN FETCH un.notification WHERE un.userSeq = :userSeq")
    List<UserNotification> findAllByUserSeq(@Param("userSeq") Integer userSeq);

}
