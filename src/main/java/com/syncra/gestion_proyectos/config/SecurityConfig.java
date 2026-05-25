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

            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                .requestMatchers("/api/v1/auth/**").permitAll()

                .anyRequest().authenticated()
            )

            .formLogin(form -> form.disable())

            .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}