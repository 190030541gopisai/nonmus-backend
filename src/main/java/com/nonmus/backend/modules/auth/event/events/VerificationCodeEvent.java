package com.nonmus.backend.modules.auth.event.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCodeEvent {
    private String email;
    private String code;
}
