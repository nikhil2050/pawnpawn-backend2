package com.pnpn.shared.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Refresh token is required")
    private String refreshToken;
}
