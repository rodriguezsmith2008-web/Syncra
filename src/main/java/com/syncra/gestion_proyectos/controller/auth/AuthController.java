package com.syncra.gestion_proyectos.controller.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

    /**
     * Servicio de autenticación
     */
    private final AuthService authService;

    /**
     * Servicio de recuperación de contraseña
     */
    private final PasswordResetService passwordResetService;

    /**
     * Registro de un nuevo usuario
     */
    @RequireRole(RoleUserEnum.ADMIN)
    @PostMapping("/register")
    public ResponseEntity<UserMessage> register(@RequestBody UserRequestDTO request) {

        try {

            UserMessage response = authService.register(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(null);

        }

    }

    /**
     * Inicio de sesión
     */
    @PostMapping("/login")
    public ResponseEntity<HttpGlobalResponse<String>> login(
            @RequestBody UserLoginDTO request) {

        try {

            HttpGlobalResponse<String> response = authService.login(request);

            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(null);

        }

    }

    /**
     * Refrescar JWT
     */
    @GetMapping("/refresh")
    public ResponseEntity<HttpGlobalResponse<String>> refreshToken(
            HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        String token = authHeader.replaceFirst("Bearer ", "");

        try {

            HttpGlobalResponse<String> response =
                    authService.refreshToken(token);

            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(null);

        }

    }

    /**
     * Envía código de recuperación
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<UserMessage> forgotPassword(
            @RequestBody ForgotPasswordDTO request) {

        try {

            UserMessage response =
                    passwordResetService.sendCode(request.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(null);

        }

    }

    /**
     * Verificar código de recuperación
     */
    @PostMapping("/verify-code")
    public ResponseEntity<UserMessage> verifyCode(
            @RequestBody VerifyCodeDTO request) {

        UserMessage response = passwordResetService.verifyCode(
                request.getEmail(),
                request.getCode());

        if (!"Código válido".equals(response.getUserMessage())) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);

        }

        return ResponseEntity.ok(response);

    }

    /**
     * Restablecer contraseña
     */
    @PostMapping("/reset-password")
    public ResponseEntity<UserMessage> resetPassword(
            @RequestBody ResetPasswordDTO request) {

        UserMessage response = passwordResetService.resetPassword(
                request.getEmail(),
                request.getCode(),
                request.getNewPassword());

        if (!"Contraseña actualizada correctamente".equals(response.getUserMessage())) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(response);

        }

        return ResponseEntity.ok(response);

    }

}