package dev.rangel.redirectservice.infrastructure.messaging;

import dev.rangel.redirectservice.application.dto.RedirectAttemptEvent;
import dev.rangel.redirectservice.application.dto.RedirectResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClickEventPublisher {

    private final KafkaTemplate<String, RedirectAttemptEvent> kafkaTemplate;
    private final Clock clock;
    private static final Logger log = LoggerFactory.getLogger(ClickEventPublisher.class);

    private static final String TOPIC_NAME = "redirect-attempts";

    public void publish(String code, RedirectResult result, String clientIp, String userAgent, String referer) {
        RedirectAttemptEvent event = mapToEvent(code, result, clientIp, userAgent, referer);

        kafkaTemplate.send(TOPIC_NAME, code, event).whenComplete((sendResult, ex) -> {
            if (ex != null) {
                log.error("Failed to publish redirect attempt event. Payload: {} due to: {}", event, ex.getMessage(), ex);
            } else {
                log.debug("Successfully published redirect attempt event for code: {} to partition: {}",
                        code, sendResult.getRecordMetadata().partition());
            }
        });
    }

    private RedirectAttemptEvent mapToEvent(String code, RedirectResult result, String ip, String ua, String ref) {
        String eventId = UUID.randomUUID().toString();
        Instant timestamp = Instant.now(clock);

        return switch (result) {
            case RedirectResult.Success success -> new RedirectAttemptEvent(
                    eventId, code, "SUCCESS", success.targetUri().toString(), null, ip, ua, ref, timestamp
            );
            case RedirectResult.Failure failure -> new RedirectAttemptEvent(
                    eventId, code, "FAILURE", null, failure.reason().name(), ip, ua, ref, timestamp
            );
        };
    }
}