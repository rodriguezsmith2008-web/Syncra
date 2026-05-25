package com.syncra.gestion_proyectos.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.Users.HttpGlobalResponse;
import com.syncra.gestion_proyectos.dto.Users.UserLoginDTO;
import com.syncra.gestion_proyectos.dto.Users.UserMessage;
import com.syncra.gestion_proyectos.dto.Users.UserRequestDTO;
import com.syncra.gestion_proyectos.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    /**
     * Servicio de autenticación
     */
    private final AuthService authService;

    /**
     * Registro de un nuevo usuario
     *
     * @param request
     * @return UserMessage con el resultado
     */
    @PostMapping("/register")
    public ResponseEntity<UserMessage> register(@RequestBody UserRequestDTO request) {
        try {
            UserMessage response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Inicio de sesión del usuario
     *
     * @param request
     * @return HttpGlobalResponse con el token JWT
     */
    @PostMapping("/login")
    public ResponseEntity<HttpGlobalResponse<String>> login(@RequestBody UserLoginDTO request) {
        try {
            HttpGlobalResponse<String> response = authService.login(request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /**
     * Refresco del token JWT
     *
     * @param request
     * @return nuevo token
     */
    @GetMapping("/refresh")
    public ResponseEntity<HttpGlobalResponse<String>> refreshToken(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String token = authHeader.replaceFirst("Bearer ", "");

        try {
            HttpGlobalResponse<String> response = authService.refreshToken(token);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}