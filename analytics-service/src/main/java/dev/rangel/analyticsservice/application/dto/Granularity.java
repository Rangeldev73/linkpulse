package dev.rangel.analyticsservice.application.dto;

import java.time.Duration;
import java.util.Locale;

public enum Granularity {
    HOUR("hour", Duration.ofHours(24), Duration.ofDays(7)),
    DAY("day", Duration.ofDays(30), Duration.ofDays(366));

    private final String unit;
    private final Duration defaultWindow;
    private final Duration maxWindow;

    Granularity(String unit, Duration defaultWindow, Duration maxWindow) {
        this.unit = unit;
        this.defaultWindow = defaultWindow;
        this.maxWindow = maxWindow;
    }

    public String unit() {
        return unit;
    }

    public Duration defaultWindow() {
        return defaultWindow;
    }

    public Duration maxWindow() {
        return maxWindow;
    }

    public static Granularity from(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidStatsQueryException("Granularity must not be blank");
        }
        try {
            return Granularity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidStatsQueryException("Unsupported granularity: " + value + ". Allowed values: hour, day");
        }
    }
}