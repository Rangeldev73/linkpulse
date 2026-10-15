package dev.rangel.redirectservice.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

public class MutableClock extends Clock {
    private Instant instant;
    private ZoneId zone;

    public MutableClock(Instant instant, ZoneId zone) {
        this.instant = instant;
        this.zone = zone;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        this.zone = zone;
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }

    public void advance(java.time.temporal.TemporalAmount amount) {
        this.instant = this.instant.plus(amount);
    }

    public void setInstant(Instant instant) {
        this.instant = instant;
    }
}