package com.ddiring.Backend_Notification.repository;

import com.ddiring.Backend_Notification.Entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Integer> {

    @Query("SELECT un FROM UserNotification un " +
            "JOIN FETCH un.notification n " +
            "WHERE un.userSeq = :userSeq")
    List<UserNotification> findAllWithNotificationByUserSeq(@Param("userSeq") String userSeq);

    @Query("SELECT un FROM UserNotification un " +
            "WHERE un.userSeq = :userSeq AND un.userNotificationSeq IN :ids")
    List<UserNotification> findAllByUserSeqAndIds(@Param("userSeq") String userSeq,
                                                  @Param("ids") List<Integer> ids);
}
