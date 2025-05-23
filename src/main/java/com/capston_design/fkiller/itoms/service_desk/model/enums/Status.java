package com.capston_design.fkiller.itoms.service_desk.model.enums;

import java.util.Arrays;

public enum Status {
    Completed, Incomplete;

    public static Status from(String value) {
        return Arrays.stream(values())
                .filter(v -> v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown category: " + value));
    }
}
