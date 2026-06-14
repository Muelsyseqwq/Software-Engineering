package com.nekocafe.security;

import com.nekocafe.auth.dto.AuthUserResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiresMinutes;

    public JwtService(
        @Value("${nekocafe.jwt.secret}") String secret,
        @Value("${nekocafe.jwt.expires-minutes:120}") long expiresMinutes
    ) {
        String normalizedSecret = secret == null ? "" : secret.trim();
        this.key = Keys.hmacShaKeyFor(sha256(normalizedSecret));
        this.expiresMinutes = expiresMinutes;
    }

    public String generateToken(AuthUserResponse user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expiresMinutes * 60);
        return Jwts.builder()
            .subject(String.valueOf(user.id()))
            .claim("username", user.username())
            .claim("nickname", user.nickname())
            .claim("roles", user.roles())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key)
            .compact();
    }

    public OffsetDateTime getExpiresAt() {
        return OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(expiresMinutes);
    }

    public AuthPrincipal parsePrincipal(String token) {
        Claims claims = parseClaims(token);
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String nickname = claims.get("nickname", String.class);
        List<?> rawRoles = claims.get("roles", List.class);
        List<String> roles = rawRoles == null
            ? List.of()
            : rawRoles.stream().map(String::valueOf).toList();
        return new AuthPrincipal(userId, username, nickname, roles);
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }
}
