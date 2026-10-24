package com.medroute.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final byte[] secret;
    private final long lifetimeSeconds;

    public JwtService(@Value("${medroute.jwt.secret}") String secret,
                      @Value("${medroute.jwt.lifetime-seconds:3600}") long lifetimeSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        this.lifetimeSeconds = lifetimeSeconds;
    }

    public String issue(AppPrincipal principal) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .setSubject(principal.username())
                .claim("displayName", principal.displayName())
                .claim("role", principal.role())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(lifetimeSeconds)));
        if (principal.hospitalId() != null) builder.claim("hospitalId", principal.hospitalId());
        return builder.signWith(Keys.hmacShaKeyFor(secret), SignatureAlgorithm.HS256).compact();
    }

    public AppPrincipal parse(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(Keys.hmacShaKeyFor(secret)).build()
                .parseClaimsJws(token).getBody();
        return new AppPrincipal(claims.getSubject(), claims.get("displayName", String.class),
                claims.get("role", String.class), claims.get("hospitalId", String.class));
    }
}