package com.syncra.gestion_proyectos.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    /**
     * Llave secreta
     */
    @Value("${security.jwt.secret-key}")
    private String secretKey;

    /**
     * Tiempo de expiración del token
     */
    @Value("${security.jwt.token-expiration}")
    private Long expiration;

    /**
     * Genera la llave de firma
     */
    private Key getSigningKey() {

        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera token JWT
     */
    public String generarToken(Long userId, String email, String role) {

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extrae todos los claims
     */
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae username/email
     */
    public String extractUsername(String token) {

        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrae id usuario
     */
    public Long extractUserId(String token) {

        return extractAllClaims(token)
                .get("userId", Long.class);
    }

    /**
     * Extrae rol
     */
    public String extractRol(String token) {

        return extractAllClaims(token)
                .get("role", String.class);
    }

    /**
     * Verifica si el token expiró
     */
    public boolean isTokenExpired(String token) {

        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    /**
     * Verifica si el token es válido
     */
    public boolean isTokenValid(String token) {

        try {

            return !isTokenExpired(token);

        } catch (Exception e) {

            return false;
        }
    }

    /**
     * Refresca un token válido
     */
    public String refreshToken(String token) {

        if (!isTokenValid(token)) {
            throw new RuntimeException("Token inválido o expirado");
        }

        Long userId = extractUserId(token);
        String email = extractUsername(token);
        String role = extractRol(token);

        return generarToken(userId, email, role);
    }
}