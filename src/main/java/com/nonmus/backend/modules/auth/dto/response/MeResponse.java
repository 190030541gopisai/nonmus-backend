package com.nonmus.backend.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeResponse {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String avatar;
    private Instant createdAt;
}