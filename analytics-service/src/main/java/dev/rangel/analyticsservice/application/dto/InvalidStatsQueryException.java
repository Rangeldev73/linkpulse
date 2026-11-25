package dev.rangel.analyticsservice.application.dto;

public class InvalidStatsQueryException extends RuntimeException {

    public InvalidStatsQueryException(String message) {
        super(message);
    }
}