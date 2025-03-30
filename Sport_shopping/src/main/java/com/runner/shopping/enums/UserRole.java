package com.runner.shopping.enums;

public enum UserRole {
    CUSTOMER(1),
    STAFF(2),
    ADMIN(3);

    private final int value;

    UserRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static UserRole fromValue(int value) {
        for (UserRole role : UserRole.values()) {
            if (role.value == value) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role value: " + value);
    }
}
