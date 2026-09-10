package com.nonmus.backend.modules.auth.dto.internal;

import lombok.*;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationCodeState {
    private String code;
    private int usedVerifyAttempts;
    private int maxVerifyAttempts;
    private long expiryInSeconds;
}
