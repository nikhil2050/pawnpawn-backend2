package com.pnpn.shared.dto.auth;

import com.pnpn.shared.dto.UserDto;
import com.pnpn.shared.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private UUID userId;
    private UserType type;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserDto user;
}
