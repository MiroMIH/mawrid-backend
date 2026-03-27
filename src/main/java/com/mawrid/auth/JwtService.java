package com.mawrid.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiry}")
    private long accessTokenExpiry;

    @Value("${app.jwt.refresh-token-expiry}")
    private long refreshTokenExpiry;

    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS = "access";
    private static final String REFRESH = "refresh";

    /** token → expiry timestamp (ms). Lazily cleaned up on reads. */
    private final ConcurrentHashMap<String, Long> blacklist = new ConcurrentHashMap<>();

    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), ACCESS, accessTokenExpiry);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), REFRESH, refreshTokenExpiry);
    }

    private String buildToken(String subject, String type, long expiryMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim(TYPE_CLAIM, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiryMs))
                .signWith(signingKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        return REFRESH.equals(extractClaim(token, claims -> claims.get(TYPE_CLAIM, String.class)));
    }

    public long getRemainingTtlMs(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return Math.max(0, expiration.getTime() - System.currentTimeMillis());
    }

    // ── In-memory Blacklist ──────────────────────────────────────

    public void blacklistToken(String token) {
        long expiresAt = System.currentTimeMillis() + getRemainingTtlMs(token);
        if (expiresAt > System.currentTimeMillis()) {
            blacklist.put(token, expiresAt);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        Long expiresAt = blacklist.get(token);
        if (expiresAt == null) return false;
        if (System.currentTimeMillis() > expiresAt) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    // ── Helpers ─────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }

    private SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
