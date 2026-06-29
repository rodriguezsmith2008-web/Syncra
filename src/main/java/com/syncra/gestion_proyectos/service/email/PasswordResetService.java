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

        if (user.getResetCodeExpires().isBefore(LocalDateTime.now())) {
            message.setUserMessage("El código ha expirado");
            return message;
        }

        // Actualiza contraseña y borra el código
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetCode(null);
        user.setResetCodeExpires(null);
        usersRepository.save(user);

        message.setUserMessage("Contraseña actualizada correctamente");
        return message;
    }

    // Construye el HTML del correo
  private String buildEmailHtml(String code) {
    StringBuilder boxes = new StringBuilder();
    for (char digit : code.toCharArray()) {
        boxes.append("""
            <td style="padding: 0 4px;">
              <div style="
                width: 48px;
                height: 60px;
                border: 2px solid #3b82f6;
                border-radius: 8px;
                text-align: center;
                line-height: 60px;
                font-size: 28px;
                font-weight: bold;
                color: #1d4ed8;
                background: #eff6ff;">
                %s
              </div>
            </td>
            """.formatted(digit));
    }

    return """
        <div style="font-family: Arial, sans-serif; background-color: #eff6ff; padding: 40px;">
          <div style="max-width: 480px; margin: 0 auto; background: white; border-radius: 12px; padding: 32px; text-align: center; border-top: 4px solid #3b82f6;">
            <h2 style="color: #1d4ed8; margin: 0 0 4px 0;">Syncra</h2>
            <p style="color: #3b82f6; font-size: 13px; margin: 0 0 24px 0;">Plataforma de gestión de proyectos</p>
            <p style="color: #374151; font-size: 15px; margin: 0 0 20px 0;">Tu código de recuperación es:</p>
            <table style="margin: 0 auto 24px auto; border-collapse: collapse;">
              <tr>%s</tr>
            </table>
            <p style="color: #6b7280; font-size: 13px; margin: 0 0 16px 0;">Este código expira en <strong style="color: #1d4ed8;">15 minutos</strong>.</p>
            <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 0 0 12px 0;">
            <p style="color: #9ca3af; font-size: 11px; margin: 0;">Si no solicitaste este código, ignora este correo.</p>
          </div>
        </div>
        """.formatted(boxes.toString());
}
}