package com.ddiring.Backend_Notification.controller;

import com.ddiring.Backend_Notification.dto.request.UserDeviceTokenRequest;
import com.ddiring.Backend_Notification.dto.response.UserDeviceTokenResponse;
import com.ddiring.Backend_Notification.enums.DeviceType;
import com.ddiring.Backend_Notification.service.UserDeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/device-tokens")
public class UserDeviceTokenController {

    private final UserDeviceTokenService userDeviceTokenService;

    @PostMapping
    public UserDeviceTokenResponse saveOrUpdateToken(@Valid @RequestBody UserDeviceTokenRequest requestDto) {
        return userDeviceTokenService.saveOrUpdateToken(requestDto);
    }

    @GetMapping("/{userSeq}/{deviceType}")
    public ResponseEntity<UserDeviceTokenResponse> getToken(
            @PathVariable String userSeq,
            @PathVariable DeviceType deviceType) {
        return userDeviceTokenService.getDeviceToken(userSeq, deviceType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
