package com.nonmus.backend.modules.auth.dto.response;

import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ForgotPasswordResponse {
    private String message;
}
