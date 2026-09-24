package com.syncra.gestion_proyectos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import java.security.Principal;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

import com.syncra.gestion_proyectos.service.auth.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Log4j2
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    autenticarConexion(accessor);
                }

                return message;
            }
        });
    }

    private void autenticarConexion(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Header Authorization ausente en conexión WebSocket");
        }

        String token = authorization.substring("Bearer ".length()).trim();
        if (!jwtService.isTokenValid(token)) {
            throw new IllegalArgumentException("Token inválido o expirado en conexión WebSocket");
        }

        String username = jwtService.extractUsername(token);
        Long userId = jwtService.extractUserId(token);
        String role = jwtService.extractRol(token);
        Principal principal = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        accessor.setUser(principal);
        accessor.getSessionAttributes().put("userId", userId);
        accessor.getSessionAttributes().put("username", username);
        accessor.getSessionAttributes().put("rolId", role);

        log.info("WebSocket STOMP autenticado: sessionId={}, userId={}, username={}, role={}",
                accessor.getSessionId(), userId, username, role);
    }

    /**
     * El límite de Spring en configureWebSocketTransport() actúa a nivel de STOMP,
     * pero Tomcat (contenedor WebSocket embebido) tiene su propio buffer por mensaje
     * que, por defecto, es de solo 8KB y rechaza el frame antes de que llegue a Spring.
     * Hay que subir ambos límites para que un pegado grande de texto no rompa la sync.
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(5 * 1024 * 1024);
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024);
        return container;
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Por defecto Spring limita cada mensaje STOMP a 64KB. Al pegar mucho texto
        // en el editor colaborativo, la actualización de Yjs (codificada en base64)
        // supera fácilmente ese límite y el mensaje se descarta, rompiendo la sincronización.
        registration
                .setMessageSizeLimit(5 * 1024 * 1024)
                .setSendBufferSizeLimit(5 * 1024 * 1024)
                .setSendTimeLimit(20000);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}