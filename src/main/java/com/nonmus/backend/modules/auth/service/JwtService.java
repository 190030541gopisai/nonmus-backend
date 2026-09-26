package com.nonmus.backend.modules.auth.service;

import com.nonmus.backend.modules.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Slf4j
@Service
public class JwtService {
    private final Key key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtService(
            @Value("${jwt.secret:defaultSecretKeyWithAtLeast256BitsSoItIsSecureEnoughForHS256Algorithm}") String secret,
            @Value("${jwt.access.expiration:900000}") long accessExpiration, // 15 mins
            @Value("${jwt.refresh.expiration:604800000}") long refreshExpiration // 7 days
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(User user) {
        log.debug("Generating access token for email: {}, username: {}", user.getEmail(), user.getUsername());
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("username", user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        log.debug("Generating refresh token for email: {}, username: {}", user.getEmail(), user.getUsername());
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("username", user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parses and validates an access token. Returns null if invalid or expired.
     */
    public String getEmailFromAccessToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            return claims.getSubject();
        } catch (Exception e) {
            log.debug("Invalid or expired JWT: {}", e.getMessage());
            return null;
        }
    }


    public boolean isExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();
            if (expiration == null || expiration.before(new Date())) {
                log.debug("Access token expired");
                return true;
            }
        } catch (Exception e) {
            log.debug("Invalid or expired JWT: {}", e.getMessage());
            return true;
        }

        return false;
    }
}
