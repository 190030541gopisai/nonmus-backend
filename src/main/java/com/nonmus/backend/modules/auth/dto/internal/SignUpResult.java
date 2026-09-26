package com.nonmus.backend.modules.auth.dto.internal;

import com.nonmus.backend.modules.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignUpResult {
    private User user;
    private String accessToken;
    private String refreshToken;
}
