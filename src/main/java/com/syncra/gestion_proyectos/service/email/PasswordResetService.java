package com.syncra.gestion_proyectos.service.email;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.users.UserMessage;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class PasswordResetService {

    private final UsersRepository usersRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    // Genera y envía el código al correo
    public UserMessage sendCode(String email) {

        UserMessage message = new UserMessage();

        Optional<UsersEntity> userOpt = usersRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            message.setUserMessage("Si el correo existe recibirás un código");
            return message;
        }

        UsersEntity user = userOpt.get();

        // Genera código de 6 dígitos
        String code = String.format("%06d", new Random().nextInt(999999));

        // Guarda en BD con expiración de 15 minutos
        user.setResetCode(code);
        user.setResetCodeExpires(LocalDateTime.now().plusMinutes(15));
        usersRepository.save(user);

        // Envía el correo con diseño HTML
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom("syncra.app@gmail.com");
            helper.setTo(email);
            helper.setSubject("Syncra — Código de recuperación");
            helper.setText(buildEmailHtml(code), true);

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            log.error("Error enviando correo: {}", e.getMessage());
        }

        message.setUserMessage("Si el correo existe recibirás un código");
        return message;
    }

    // Verifica que el código sea válido
    public UserMessage verifyCode(String email, String code) {

        UserMessage message = new UserMessage();

        Optional<UsersEntity> userOpt = usersRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            message.setUserMessage("Código inválido");
            return message;
        }

        UsersEntity user = userOpt.get();

        if (user.getResetCode() == null || !user.getResetCode().equals(code)) {
            message.setUserMessage("Código inválido");
            return message;
        }

        if (user.getResetCodeExpires().isBefore(LocalDateTime.now())) {
            message.setUserMessage("El código ha expirado");
            return message;
        }

        message.setUserMessage("Código válido");
        return message;
    }

   // Cambia la contraseña si el código es válido
public UserMessage resetPassword(String email, String code, String newPassword) {

    UserMessage message = new UserMessage();

    Optional<UsersEntity> userOpt = usersRepository.findByEmail(email);

    if (userOpt.isEmpty()) {
        message.setUserMessage("Código inválido");
        return message;
    }

    UsersEntity user = userOpt.get();

    if (user.getResetCode() == null || !user.getResetCode().equals(code)) {
        message.setUserMessage("Código inválido");
        return message;
    }

    if (user.getResetCodeExpires() == null
            || user.getResetCodeExpires().isBefore(LocalDateTime.now())) {

        message.setUserMessage("El código ha expirado");
        return message;
    }

    // ===== VALIDACIONES DE CONTRASEÑA =====

    if (newPassword == null || newPassword.length() < 8) {
        message.setUserMessage("La contraseña debe tener mínimo 8 caracteres");
        return message;
    }

    if (!newPassword.matches(".*\\d.*")) {
        message.setUserMessage("La contraseña debe contener al menos un número");
        return message;
    }

    if (!newPassword.matches(".*[!@#$%^&*].*")) {
        message.setUserMessage("La contraseña debe contener un carácter especial");
        return message;
    }

    // ================================

    user.setPassword(passwordEncoder.encode(newPassword));
    user.setResetCode(null);
    user.setResetCodeExpires(null);
    user.setMustChangePassword(false);
    user.setTempPasswordExpiresAt(null);

    usersRepository.save(user);

    message.setUserMessage("Contraseña actualizada correctamente");

    return message;
}

    // Construye el HTML del correo
    private String buildEmailHtml(String code) {

        StringBuilder boxes = new StringBuilder();

        for (char digit : code.toCharArray()) {
            boxes.append("""
                    <td style="padding:4px;">
                        <table role="presentation"
                               cellpadding="0"
                               cellspacing="0"
                               border="0"
                               width="48"
                               height="60"
                               style="
                                   width:48px;
                                   height:60px;
                                   border:2px solid #3b82f6;
                                   border-radius:8px;
                                   background-color:#eff6ff;
                                   border-collapse:separate;">
                            <tr>
                                <td align="center"
                                    valign="middle"
                                    style="
                                        width:48px;
                                        height:60px;
                                        text-align:center;
                                        vertical-align:middle;
                                        font-family:Arial,sans-serif;
                                        font-size:28px;
                                        font-weight:bold;
                                        color:#1d4ed8;
                                        line-height:28px;">
                                    %s
                                </td>
                            </tr>
                        </table>
                    </td>
                    """.formatted(digit));
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>

                <body style="margin:0;padding:0;background:#eff6ff;">

                    <table role="presentation"
                           cellpadding="0"
                           cellspacing="0"
                           border="0"
                           width="100%%"
                           style="background:#eff6ff;padding:40px 0;">

                        <tr>
                            <td align="center">

                                <table role="presentation"
                                       cellpadding="0"
                                       cellspacing="0"
                                       border="0"
                                       width="480"
                                       style="
                                           width:480px;
                                           max-width:480px;
                                           background:#ffffff;
                                           border-radius:12px;
                                           border-top:4px solid #3b82f6;
                                           font-family:Arial,sans-serif;">

                                    <tr>
                                        <td align="center" style="padding:32px;">

                                            <h2 style="
                                                margin:0;
                                                color:#1d4ed8;
                                                font-size:28px;">
                                                Syncra
                                            </h2>

                                            <p style="
                                                margin:8px 0 28px;
                                                color:#3b82f6;
                                                font-size:13px;">
                                                Plataforma de gestión de proyectos
                                            </p>

                                            <p style="
                                                margin:0 0 24px;
                                                color:#374151;
                                                font-size:16px;">
                                                Tu código de recuperación es:
                                            </p>

                                            <table role="presentation"
                                                   cellpadding="0"
                                                   cellspacing="0"
                                                   border="0"
                                                   align="center"
                                                   style="margin:0 auto 24px auto;">
                                                <tr>
                                                    %s
                                                </tr>
                                            </table>

                                            <p style="
                                                margin:0 0 20px;
                                                color:#6b7280;
                                                font-size:14px;">
                                                Este código expira en
                                                <strong style="color:#1d4ed8;">
                                                    15 minutos
                                                </strong>.
                                            </p>

                                            <hr style="
                                                border:none;
                                                border-top:1px solid #e5e7eb;
                                                margin:20px 0;">

                                            <p style="
                                                margin:0;
                                                color:#9ca3af;
                                                font-size:12px;">
                                                Si no solicitaste este código, puedes ignorar este correo.
                                            </p>

                                        </td>
                                    </tr>

                                </table>

                            </td>
                        </tr>

                    </table>

                </body>
                </html>
                """.formatted(boxes.toString());
    }
}