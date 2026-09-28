package com.syncra.gestion_proyectos.security;

import java.util.Arrays;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.access.AccessDeniedException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RoleInterceptor implements HandlerInterceptor{

    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;

    public RoleInterceptor(
            JsonAccessDeniedHandler accessDeniedHandler,
            JsonAuthenticationEntryPoint authenticationEntryPoint) {
        this.accessDeniedHandler = accessDeniedHandler;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RequireRole annotation = method.getMethodAnnotation(RequireRole.class);

        if (annotation == null) {
            annotation = method.getBeanType().getAnnotation(RequireRole.class);
        }

        if (annotation == null) {
            return true;
        }

        Object roleObject = request.getAttribute("rolId");

        if (!(roleObject instanceof String role)) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new AuthenticationCredentialsNotFoundException("Usuario no autenticado"));
            return false;

        }

        boolean hasRole = Arrays.stream(annotation.value()).anyMatch(r -> r.name().equals(role));

        if (!hasRole) {
            accessDeniedHandler.handle(
                    request,
                    response,
                    new AccessDeniedException("Acceso denegado"));
            return false;

        }

        return true;
    }

}