package com.nonmus.backend.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "Email or username is required")
    @Size(max = 255, message = "Email or username must not exceed 255 characters")
    private String credential;

    @NotBlank(message = "Password is required")
    @Size(max = 64, message = "Password must not exceed 64 characters")
    private String password;
}