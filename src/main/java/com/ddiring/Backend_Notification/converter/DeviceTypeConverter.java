package com.ddiring.Backend_Notification.converter;

import com.ddiring.Backend_Notification.enums.DeviceType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class DeviceTypeConverter implements AttributeConverter<DeviceType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(DeviceType attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public DeviceType convertToEntityAttribute(Integer dbData) {
        return dbData != null ? DeviceType.fromCode(dbData) : null;
    }
}