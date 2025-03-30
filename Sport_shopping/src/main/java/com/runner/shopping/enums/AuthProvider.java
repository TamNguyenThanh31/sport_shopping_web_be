package com.runner.shopping.enums;

public enum AuthProvider {
    LOCAL(0),
    GOOGLE(1);

    private final int value;

    AuthProvider(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AuthProvider fromValue(int value) {
        for (AuthProvider provider : AuthProvider.values()) {
            if (provider.value == value) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown provider value: " + value);
    }
}
