package com.ddiring.Backend_Notification.converter;

import com.ddiring.Backend_Notification.enums.NotificationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class NotificationStatusConverter implements AttributeConverter<NotificationStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(NotificationStatus attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public NotificationStatus convertToEntityAttribute(Integer dbData) {
        return dbData != null ? NotificationStatus.fromCode(dbData) : null;
    }
}
