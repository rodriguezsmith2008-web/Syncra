package com.syncra.gestion_proyectos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.syncra.gestion_proyectos.security.JsonAccessDeniedHandler;
import com.syncra.gestion_proyectos.security.JsonAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(
            JsonAccessDeniedHandler accessDeniedHandler,
            JsonAuthenticationEntryPoint authenticationEntryPoint) {
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

            // Desactiva CSRF (no aplica, usan JWT sin sesiones/cookies)
            .csrf(csrf -> csrf.disable())

            // Habilita CORS usando el bean CorsFilter definido en CorsConfig
            .cors(cors -> {})

            // Sin sesiones: cada request se autentica vía JWT
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // La autorización real la hace JwtValidationFilter (rutas públicas por
            // path.endsWith()) + @RequireRole por endpoint — no Spring Security aquí
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
            )

                .exceptionHandling(exception -> exception
                    .accessDeniedHandler(accessDeniedHandler)
                    .authenticationEntryPoint(authenticationEntryPoint))

            .formLogin(form -> form.disable())
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}