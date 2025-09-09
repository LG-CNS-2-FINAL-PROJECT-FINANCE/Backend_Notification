package com.ddiring.Backend_Notification.repository;

import com.ddiring.Backend_Notification.Entity.UserDeviceToken;
import com.ddiring.Backend_Notification.enums.DeviceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceTokenRepository extends JpaRepository<UserDeviceToken, Integer> {

    Optional<UserDeviceToken> findByUserSeqAndDeviceType(String userSeq, DeviceType deviceType);

    List<UserDeviceToken> findAllByUserSeq(String userSeq);
}