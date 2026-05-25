package com.syncra.gestion_proyectos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

            // Desactiva CSRF
            .csrf(csrf -> csrf.disable())

            // Permite TODAS las requests
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll()
            )

            // Desactiva login default
            .formLogin(form -> form.disable())

            // Desactiva basic auth
            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}