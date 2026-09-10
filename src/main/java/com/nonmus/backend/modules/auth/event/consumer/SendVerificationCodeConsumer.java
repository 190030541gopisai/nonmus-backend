package com.nonmus.backend.modules.auth.event.consumer;

import com.nonmus.backend.config.KafkaConfig;
import com.nonmus.backend.modules.auth.dto.request.VerificationCodeTarget;
import com.nonmus.backend.modules.auth.event.events.VerificationCodeEvent;
import com.nonmus.backend.modules.auth.sender.VerificationCodeSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendVerificationCodeConsumer {

    private final VerificationCodeSender verificationCodeSender;

    @KafkaListener(
            topics = KafkaConfig.VERIFICATION_CODE_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:nonmus-auth-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void send(VerificationCodeEvent event) {
        log.info("Received verification code event for email: {}", event.getEmail());
        verificationCodeSender.send(
                VerificationCodeTarget.builder()
                        .email(event.getEmail())
                        .code(event.getCode())
                        .build()
        );
    }
}

