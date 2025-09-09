package com.ddiring.Backend_Notification.service;

import com.ddiring.Backend_Notification.Entity.UserDeviceToken;
import com.ddiring.Backend_Notification.dto.request.UserDeviceTokenRequest;
import com.ddiring.Backend_Notification.dto.response.UserDeviceTokenResponse;
import com.ddiring.Backend_Notification.enums.DeviceType;
import com.ddiring.Backend_Notification.repository.UserDeviceTokenRepository;
import jakarta.transaction.Transactional;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserDeviceTokenService {
    private final UserDeviceTokenRepository repository;

    @Transactional
    public UserDeviceTokenResponse saveOrUpdateToken(UserDeviceTokenRequest dto) {
        UserDeviceToken token = repository.findByUserSeqAndDeviceType(dto.getUserSeq(), dto.getDeviceType())
                .map(existing -> existing.toBuilder()
                        .deviceToken(dto.getFcmToken())
                        .updatedAt(LocalDateTime.now())
                        .build())
                .orElse(UserDeviceToken.builder()
                        .userSeq(dto.getUserSeq())
                        .deviceType(dto.getDeviceType())
                        .deviceToken(dto.getFcmToken())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build());

        return toResponse(repository.save(token));
    }

    @Transactional
    public List<String> getDeviceTokens(String userSeq) {
        return repository.findAllByUserSeq(userSeq).stream()
                .map(UserDeviceToken::getDeviceToken)
                .toList();
    }

    @Transactional
    public Optional<UserDeviceTokenResponse> getDeviceToken(String userSeq, DeviceType deviceType) {
        return repository.findByUserSeqAndDeviceType(userSeq, deviceType)
                .map(this::toResponse);
    }

    private UserDeviceTokenResponse toResponse(UserDeviceToken entity) {
        return UserDeviceTokenResponse.builder()
                .userDeviceTokenId(entity.getUserDeviceTokenId())
                .userSeq(entity.getUserSeq())
                .fcmToken(entity.getDeviceToken())
                .deviceType(entity.getDeviceType())
                .build();
    }
}