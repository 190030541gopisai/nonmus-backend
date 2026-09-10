package com.nonmus.backend.modules.auth.event.publisher;

import com.nonmus.backend.config.KafkaConfig;
import com.nonmus.backend.modules.auth.event.events.VerificationCodeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaVerificationCodeEventPublisher
        implements VerificationCodeEventPublisher {

    private final KafkaTemplate<String, VerificationCodeEvent> kafkaTemplate;

    @Override
    public void publish(VerificationCodeEvent event) {
        kafkaTemplate.send(KafkaConfig.VERIFICATION_CODE_TOPIC, event.getEmail(), event);
    }
}