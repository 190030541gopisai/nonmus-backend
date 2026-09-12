package com.nonmus.backend.modules.auth.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyResult {
    private boolean verified;
    private String verificationToken;
}
