package com.nonmus.backend.modules.auth.dto.internal;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {
    private String email;
    private String token;
}
