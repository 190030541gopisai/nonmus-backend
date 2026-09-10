package com.nonmus.backend.modules.auth.event.publisher;

import com.nonmus.backend.modules.auth.event.events.VerificationCodeEvent;

public interface VerificationCodeEventPublisher {
    public void publish(VerificationCodeEvent event);
}
