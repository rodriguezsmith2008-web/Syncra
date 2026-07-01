package com.syncra.gestion_proyectos.service.email;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendAccessApprovedEmail(String toEmail, String firstName, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Tu acceso a Syncra ha sido aprobado");
            helper.setText(buildEmailBody(firstName, toEmail, tempPassword), true);

            mailSender.send(message);
            log.info("Correo de aprobación enviado a {}", toEmail);

        } catch (MessagingException e) {
            log.error("Error enviando correo a {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildEmailBody(String firstName, String email, String tempPassword) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px;">
                    <h2 style="color: #4A90D9;">¡Bienvenido/a a Syncra</h2>
                    <p>Tu solicitud de acceso ha sido <strong>aprobada</strong>.
                    Ya puedes ingresar a la plataforma con las siguientes credenciales:</p>

                    <div style="background: #f4f4f4; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <p><strong>✉️ Correo:</strong> %s</p>
                        <p><strong>🔑 Contraseña temporal:</strong> %s</p>
                    </div>

                    <p style="color: #e74c3c;">
                        ⚠️ Por seguridad, se debe cambiar tu contraseña
                        después de tu primer inicio de sesión.
                    </p>

                    <hr style="margin-top: 30px;">
                    <p style="color: #999; font-size: 12px;">
                        SENA — Centro de Comercio y Turismo, Armenia, Quindío<br>
                        Análisis y Desarrollo de Software
                    </p>
                </div>
                """.formatted(email, tempPassword); 
    }
}