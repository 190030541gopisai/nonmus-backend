package com.nonmus.backend.modules.auth.dto.internal;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendState {
    private long cooldown;
    private int usedResendAttempts;
    private int maxResendAttempts;
}
