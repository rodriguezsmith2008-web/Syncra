package com.syncra.gestion_proyectos.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.syncra.gestion_proyectos.service.auth.JwtService;
import com.syncra.gestion_proyectos.security.JsonAuthenticationEntryPoint;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
@Component
public class JwtValidationFilter extends OncePerRequestFilter {

    /**
     * Servicio JWT
     */
    private final JwtService jwtService;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Header Authorization
        String authHeader = request.getHeader("Authorization");

        // Verifica formato Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            authenticationEntryPoint.commence(request, response,
                new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Header Authorization is missing"));
            return;
        }

        // Extrae token
        String token = authHeader.replaceFirst("Bearer ", "");

        try {

            // Validación correcta
            if (jwtService.isTokenValid(token)) {

                // Extrae datos
                String username = jwtService.extractUsername(token);
                Long userId = jwtService.extractUserId(token);
                String rolId = jwtService.extractRol(token);

                // Guarda atributos
                request.setAttribute("username", username);
                request.setAttribute("userId", userId);
                request.setAttribute("rolId", rolId);

                // Continúa request
                filterChain.doFilter(request, response);

            } else {
                authenticationEntryPoint.commence(request, response,
                    new org.springframework.security.authentication.BadCredentialsException(
                        "Token invalid or expired"));
            }

        } catch (Exception e) {
            authenticationEntryPoint.commence(request, response,
                new org.springframework.security.authentication.BadCredentialsException(
                    "Validation failed", e));

            log.error("JWT Error: {}", e.getMessage());
        }
    }

    @Override
protected boolean shouldNotFilter(HttpServletRequest request) {

    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }

    String path = request.getRequestURI();
    String method = request.getMethod();

    if (path.contains("/ws")) {
        return true;
    }

    if (path.endsWith("/access-requests") && method.equals("POST")) {
        return true;
    }

        if (path.matches(".*/doc-templates(?:/\\d+)?") && method.equals("GET")) {
            return true;
        }

    return path.endsWith("/auth/login")
            || path.endsWith("/auth/refresh")
            || path.endsWith("/auth/forgot-password")
            || path.endsWith("/auth/verify-code")
            || path.endsWith("/auth/reset-password")
            || path.endsWith("/contact")
            || path.endsWith("/public/stats");
}
}