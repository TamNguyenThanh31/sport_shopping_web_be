package com.runner.shopping.enums.converter;

import com.runner.shopping.enums.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus attribute) {
        return attribute != null ? attribute.name().toLowerCase() : null;
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? PaymentStatus.valueOf(dbData.toUpperCase()) : null;
    }
}
