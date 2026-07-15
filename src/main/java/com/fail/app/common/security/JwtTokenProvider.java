package com.fail.app.common.security;

import com.fail.app.common.config.JwtProperties;
import com.fail.app.domain.user.entity.User;
import com.fail.app.domain.user.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(jwtProperties.accessTokenValiditySeconds());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("email", user.getEmail())
                .claim("nickname", user.getNickname())
                .claim("role", user.getRole().name())
                .signWith(secretKey)
                .compact();
    }

    public CurrentUser getCurrentUser(String token) {
        Claims claims = parseClaims(token);
        return new CurrentUser(
                Long.parseLong(claims.getSubject()),
                claims.get("email", String.class),
                claims.get("nickname", String.class),
                UserRole.valueOf(claims.get("role", String.class))
        );
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
