package com.nonmus.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ForgotPasswordRequest {
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = ".*\\d.*",
            message = "Password must contain a number"
    )
    @Pattern(
            regexp = ".*[^a-zA-Z0-9].*",
            message = "Password must contain a special character"
    )
    private String password;

    private String confirmPassword;
}
