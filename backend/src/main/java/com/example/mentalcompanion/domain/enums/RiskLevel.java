package com.example.mentalcompanion.domain.enums;

public enum RiskLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int weight;

    RiskLevel(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public static RiskLevel max(RiskLevel left, RiskLevel right) {
        if (left == null) {
            return right == null ? LOW : right;
        }
        if (right == null) {
            return left;
        }
        return left.weight >= right.weight ? left : right;
    }

    public static RiskLevel safeValueOf(String value, RiskLevel fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return RiskLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}

