package com.syncra.gestion_proyectos.filter;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.syncra.gestion_proyectos.service.auth.JwtService;

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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Header Authorization
        String authHeader = request.getHeader("Authorization");

        // Verifica formato Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter()
                    .write("{\"error\": \"Header Authorization is missing\"}");

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

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");

                response.getWriter()
                        .write("{\"error\": \"Token invalid or expired\"}");
            }

        } catch (Exception e) {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            response.getWriter()
                    .write("{\"error\": \"Validation failed\"}");

            log.error("JWT Error: {}", e.getMessage());
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // POST /api/v1/access-requests — público, cualquiera puede solicitar acceso
        if (path.equals("/api/v1/access-requests") && method.equals("POST")) {
            return true;
        }

        // Auth siempre público
        return path.startsWith("/api/v1/auth");
    }
}