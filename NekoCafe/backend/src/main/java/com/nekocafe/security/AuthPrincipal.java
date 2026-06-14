package com.nekocafe.security;

import java.util.List;

public record AuthPrincipal(
    Long userId,
    String username,
    String nickname,
    List<String> roles
) {
}
