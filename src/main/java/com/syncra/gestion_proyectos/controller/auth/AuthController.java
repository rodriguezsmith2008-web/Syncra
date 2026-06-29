package com.syncra.gestion_proyectos.controller.auth;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.passwordreset.ForgotPasswordDTO;
import com.syncra.gestion_proyectos.dto.passwordreset.ResetPasswordDTO;
import com.syncra.gestion_proyectos.dto.passwordreset.VerifyCodeDTO;
import com.syncra.gestion_proyectos.dto.users.HttpGlobalResponse;
import com.syncra.gestion_proyectos.dto.users.UserLoginDTO;
import com.syncra.gestion_proyectos.dto.users.UserMessage;
import com.syncra.gestion_proyectos.dto.users.UserRequestDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.auth.AuthService;
import com.syncra.gestion_proyectos.service.email.PasswordResetService;

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
    private final PasswordResetService passwordResetService;

    /**
     * Registro de un nuevo usuario
     *
     * @param request
     * @return UserMessage con el resultado
     */

    @RequireRole(RoleUserEnum.ADMIN)
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

    // recuperacion de contraseña

    @PostMapping("/forgot-password")
    public ResponseEntity<UserMessage> forgotPassword(@RequestBody ForgotPasswordDTO request) {
        try {
            UserMessage response = passwordResetService.sendCode(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    //verifica que el codigo sea correcto 
    @PostMapping("/verify-code")
    public ResponseEntity<UserMessage> verifyCode(@RequestBody VerifyCodeDTO request) {
        try {
            UserMessage response = passwordResetService.verifyCode(request.getEmail(), request.getCode());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    //guarda la nueva contraseña
    @PostMapping("/reset-password")
    public ResponseEntity<UserMessage> resetPassword(@RequestBody ResetPasswordDTO request) {
        try {
            UserMessage response = passwordResetService.resetPassword(
                    request.getEmail(), request.getCode(), request.getNewPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}