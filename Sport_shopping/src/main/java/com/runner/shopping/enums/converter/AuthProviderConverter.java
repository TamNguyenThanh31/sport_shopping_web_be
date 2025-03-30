package com.runner.shopping.enums.converter;

import com.runner.shopping.enums.AuthProvider;
import jakarta.persistence.AttributeConverter;

public class AuthProviderConverter implements AttributeConverter<AuthProvider, Integer> {
    @Override
    public Integer convertToDatabaseColumn(AuthProvider provider) {
        if (provider == null) {
            return null;
        }
        return provider.getValue();
    }

    @Override
    public AuthProvider convertToEntityAttribute(Integer value) {
        if (value == null) {
            return null;
        }
        return AuthProvider.fromValue(value);
    }
}
