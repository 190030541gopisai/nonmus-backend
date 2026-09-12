package com.nonmus.backend.modules.auth.event.publisher;

import com.nonmus.backend.config.KafkaConfig;
import com.nonmus.backend.modules.auth.event.events.VerificationCodeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaVerificationCodeEventPublisher
        implements VerificationCodeEventPublisher {

    private final KafkaTemplate<String, VerificationCodeEvent> kafkaTemplate;

    @Override
    public void publish(VerificationCodeEvent event) {
        log.debug("Publishing verification code event to Kafka for email: {}", event.getEmail());
        kafkaTemplate.send(KafkaConfig.VERIFICATION_CODE_TOPIC, event.getEmail(), event);
        log.info("Verification code event published to Kafka topic: {}", KafkaConfig.VERIFICATION_CODE_TOPIC);
    }
}