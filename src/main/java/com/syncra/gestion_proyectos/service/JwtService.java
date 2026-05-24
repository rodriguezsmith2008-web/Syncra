package com.syncra.gestion_proyectos.service;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    /**
     * Clave secreta inyectada desde el archivo de configuración YAML
     */
    @Value("${security.jwt.secret-key}")
    private String KeySecret;

    /**
     * Tiempo de expiración del token inyectado desde el archivo de configuración YAML
     */
    @Value("${security.jwt.token-expiration}")
    Long tokenExpiration;

    /**
     * Transforma la clave secreta de String BASE64 a un objeto SecretKey
     *
     * @return clave secreta para firmar el token
     */
    private SecretKey getSignKey() {
        byte[] keyBytes = Decoders.BASE64.decode(KeySecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera el token JWT al iniciar sesión con los datos del usuario
     *
     * @param userId
     * @param Name
     * @param rol
     * @return token JWT firmado
     */
    public String generarToken(long userId, String Name, String rol) {
        return Jwts.builder()
                .claims(Map.of("userId", userId, "Rol", rol))
                .subject(Name)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSignKey())
                .compact();
    }

    /**
     * Verifica si el token JWT es válido y no ha expirado
     *
     * @param token
     * @return true si el token es válido, false si es inválido o expirado
     */
    public boolean isTokenInvalid(String token) {
        try {
            Jwts.parser().verifyWith(getSignKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Extrae un claim específico del token JWT aplicando una función de resolución
     *
     * @param <T>
     * @param token
     * @param resolver
     * @return valor del claim extraído
     */
    public <T> T extractClaims(String token, Function<Claims, T> resolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return resolver.apply(claims);
    }

    /**
     * Extrae el email del usuario almacenado como subject en el token
     *
     * @param token
     * @return
     */
    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    /**
     * Extrae el identificador único del usuario almacenado en el token
     *
     * @param token token JWT
     * @return id del usuario
     */
    public Long extractUserId(String token) {
        return extractClaims(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * Extrae el rol del usuario almacenado en el token
     *
     * @param token token JWT
     * @return rol del usuario
     */
    public String extractRol(String token) {
        return extractClaims(token, claims -> claims.get("Rol", String.class));
    }

    /**
     * Genera un nuevo token JWT a partir de uno existente que aún no ha expirado
     *
     * @param token
     * @return
     * @throws Exception si el token está expirado o es inválido
     */
    public String refreshToken(String token) throws Exception {
        Claims claims;

        try {
            claims = Jwts.parser()
                    .verifyWith(getSignKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new Exception("Token is expired" + e.getMessage());
        } catch (JwtException e) {
            throw new Exception("Token is invalid" + e.getMessage());
        }

        return generarToken(claims.get("userId", Long.class), claims.getSubject(), claims.get("Rol", String.class));
    }
}