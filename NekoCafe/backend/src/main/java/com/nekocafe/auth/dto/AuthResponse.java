package com.nekocafe.auth.dto;

import java.time.OffsetDateTime;

public record AuthResponse(
    String token,
    String tokenType,
    OffsetDateTime expiresAt,
    AuthUserResponse user
) {
}
