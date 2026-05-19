package com.example.mentalcompanion.domain.enums;

public enum IntentType {
    CHAT,
    CONSULT,
    KNOWLEDGE,
    HIGH_RISK;

    public static IntentType safeValueOf(String value, IntentType fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return IntentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}

