package com.runner.shopping.enums.converter;

import com.runner.shopping.enums.UserRole;
import jakarta.persistence.AttributeConverter;

public class UserRoleConverter implements AttributeConverter<UserRole, Integer> {
    @Override
    public Integer convertToDatabaseColumn(UserRole userRole) {
        if (userRole == null) {
            return null;
        }
        return userRole.getValue();
    }

    @Override
    public UserRole convertToEntityAttribute(Integer value) {
        if (value == null) {
            return null;
        }
        return UserRole.fromValue(value);
    }
}
