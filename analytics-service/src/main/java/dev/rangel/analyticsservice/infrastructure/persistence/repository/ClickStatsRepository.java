package dev.rangel.analyticsservice.infrastructure.persistence.repository;

import dev.rangel.analyticsservice.domain.model.ClickEventEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ClickStatsRepository extends Repository<ClickEventEntity, Long> {

    /**
     * Counts successful redirects of one code per time bucket.
     * The window is closed at the start and open at the end ([start, end)), so an event
     * exactly on a boundary is counted in one bucket only. Served by idx_click_events_code_occurred_at.
     * Scoped to the link owner: events of other owners (or legacy events without an owner) are never counted.
     *
     * @param granularity a date_trunc unit, validated by the caller ("hour" or "day")
     */
    @Query(value = """
            SELECT CAST(EXTRACT(EPOCH FROM date_trunc(CAST(:granularity AS text), occurred_at, 'UTC')) AS BIGINT) AS bucket,
                   COUNT(*) AS clicks
            FROM tb_click_events
            WHERE code = :code
              AND owner_id = :ownerId
              AND status = 'SUCCESS'
              AND occurred_at >= :start
              AND occurred_at < :end
            GROUP BY bucket
            ORDER BY bucket
            """, nativeQuery = true)
    List<ClickBucketProjection> countClicksByBucket(@Param("code") String code,
                                                    @Param("ownerId") String ownerId,
                                                    @Param("granularity") String granularity,
                                                    @Param("start") Instant start,
                                                    @Param("end") Instant end);
}
