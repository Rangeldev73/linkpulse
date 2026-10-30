package dev.rangel.analyticsservice.infrastructure.messaging;

import dev.rangel.analyticsservice.application.dto.RedirectAttemptEvent;
import dev.rangel.analyticsservice.application.service.ClickEventPersistenceService;
import dev.rangel.analyticsservice.domain.model.ClickEventEntity;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClickEventConsumer {

    private final ClickEventPersistenceService persistenceService;
    private static final Logger log = LoggerFactory.getLogger(ClickEventConsumer.class);

    @KafkaListener(topics = "redirect-attempts", groupId = "analytics-service-group")
    public void consume(RedirectAttemptEvent event) {
        log.debug("Received redirect attempt event with ID: {} for code: {}", event.eventId(), event.code());

        ClickEventEntity entity = ClickEventEntity.builder()
                .eventId(event.eventId())
                .code(event.code())
                .status(event.status())
                .targetUrl(event.targetUrl())
                .failureReason(event.failureReason())
                .clientIp(event.clientIp())
                .userAgent(event.userAgent())
                .referer(event.referer())
                .occurredAt(event.timestamp())
                .build();

        try {
            persistenceService.saveEventIsolated(entity);
            log.debug("Successfully persisted click event: {}", event.eventId());
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEventViolation(e)) {
                log.warn("Duplicate event detected (already processed): {}. Skipping safely.", event.eventId());
            } else {
                log.error("Unexpected database integrity violation while processing event: {}", event.eventId(), e);
                throw e;
            }
        }
    }

    private boolean isDuplicateEventViolation(DataIntegrityViolationException e) {
        Throwable rootCause = e.getMostSpecificCause();
        String message = rootCause.getMessage();
        return message != null && message.contains("uk_click_events_event_id");
    }
}