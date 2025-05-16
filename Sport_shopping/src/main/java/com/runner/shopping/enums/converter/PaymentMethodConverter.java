package com.runner.shopping.enums.converter;

import com.runner.shopping.enums.PaymentMethod;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PaymentMethodConverter implements AttributeConverter<PaymentMethod, String> {

    @Override
    public String convertToDatabaseColumn(PaymentMethod attribute) {
        return attribute != null ? attribute.name() : null;
    }

    @Override
    public PaymentMethod convertToEntityAttribute(String dbData) {
        return dbData != null ? PaymentMethod.valueOf(dbData) : null;
    }
}
