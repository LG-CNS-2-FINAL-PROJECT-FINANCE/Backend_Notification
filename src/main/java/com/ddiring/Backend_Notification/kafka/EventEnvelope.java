package com.ddiring.Backend_Notification.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventEnvelope<T> {
    private String eventId;
    private Instant timestamp;
    private T payload;
}