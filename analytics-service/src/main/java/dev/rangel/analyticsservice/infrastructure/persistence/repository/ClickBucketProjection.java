package dev.rangel.analyticsservice.infrastructure.persistence.repository;

/**
 * One row of the click-count query: a time bucket and how many clicks fell into it.
 * {@code bucket} is the bucket start as UTC epoch seconds (a plain number avoids
 * any ambiguity about how the JDBC driver maps timestamps).
 */
public interface ClickBucketProjection {

    Long getBucket();

    Long getClicks();
}
