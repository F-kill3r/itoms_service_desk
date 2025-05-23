package com.capston_design.fkiller.itoms.service_desk.model.enums;

import java.util.Arrays;

public enum Priority {
    URGENT,
    RELAXED;

    public static Priority from(String value) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + value));
    }
}
