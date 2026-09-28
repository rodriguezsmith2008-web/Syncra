package com.syncra.gestion_proyectos.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.contact.ContactRequestDto;

@Service
public class ContactService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String projectEmail;

    public ContactService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendContactMessage(ContactRequestDto dto) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(projectEmail);
            helper.setSubject("Nuevo mensaje de contacto - " + dto.getSubject());
            helper.setFrom(projectEmail);
            helper.setReplyTo(dto.getEmail());

            String body = buildContactEmailBody(dto);
            helper.setText(body, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("error al enviar el mensaje de contacto", e);
        }
    }

    private String buildContactEmailBody(ContactRequestDto dto) {
        return """
                <html>
                <body style="margin:0; padding:0; background-color:#f4f4f7;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f4f4f7; padding:24px 0;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" style="max-width:520px; background-color:#ffffff; border-radius:10px; overflow:hidden; font-family:Arial, Helvetica, sans-serif;" cellpadding="0" cellspacing="0">

                                    <!-- Header -->
                                    <tr>
                                        <td style="padding:24px 28px;">
                                            <span style="font-size:20px; font-weight:bold; color:#111827;">&#9993; Nuevo mensaje de contacto</span>
                                        </td>
                                    </tr>

                                    <!-- Estado + fecha -->
                                    <tr>
                                        <td style="padding:0 28px 20px 28px;">
                                            <table role="presentation" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="background-color:#eff6ff; border-radius:20px; padding:6px 14px;">
                                                        <span style="color:#2563eb; font-size:13px; font-weight:bold;">&#9679; Estado: Nuevo</span>
                                                    </td>
                                                    <td style="padding-left:14px; color:#6b7280; font-size:13px;">%s</td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Contenido -->
                                    <tr>
                                        <td style="padding:0 28px;">
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="border-left:4px solid #2563eb;">
                                                <tr>
                                                    <td style="padding:16px 0 16px 20px;">

                                                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                                            <tr>
                                                                <td style="padding:8px 0; font-size:13px; color:#6b7280;">Remitente</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:0 0 14px 0; font-size:16px; color:#111827; font-weight:bold;">%s %s</td>
                                                            </tr>

                                                            <tr>
                                                                <td style="padding:8px 0; font-size:13px; color:#6b7280;">Correo electr&oacute;nico</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:0 0 14px 0; font-size:15px; color:#111827; font-weight:bold;">%s</td>
                                                            </tr>

                                                            <tr>
                                                                <td style="padding:8px 0; font-size:13px; color:#6b7280;">Asunto</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:0 0 14px 0; font-size:15px; color:#2563eb; font-weight:bold;">%s</td>
                                                            </tr>

                                                            <tr>
                                                                <td style="padding:8px 0; font-size:13px; color:#6b7280;">Mensaje</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:14px 16px; font-size:14px; color:#111827; background-color:#f9fafb; border-radius:8px; line-height:1.6;">%s</td>
                                                            </tr>
                                                        </table>

                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Boton responder -->
                                    <tr>
                                        <td style="padding:24px 28px;">
                                            <table role="presentation" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="background-color:#2563eb; border-radius:8px;">
                                                        <a href="mailto:%s?subject=Re: %s" style="display:inline-block; padding:12px 24px; font-size:14px; font-weight:bold; color:#ffffff; text-decoration:none;">&#8617; Responder</a>
                                                    </td>
                                                </tr>
                                            </table>
                                        </td>
                                    </tr>

                                    <!-- Footer -->
                                    <tr>
                                        <td style="padding:18px 28px; background-color:#f4f4f7;">
                                            <span style="font-size:12px; color:#9ca3af;">Syncra - SENA Centro de Comercio y Turismo</span>
                                        </td>
                                    </tr>

                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """
                .formatted(
                        java.time.LocalDateTime.now()
                                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm a")),
                        dto.getFirstName(), dto.getLastName(),
                        dto.getEmail(),
                        dto.getSubject(),
                        dto.getMessage(),
                        dto.getEmail(), dto.getSubject());
    }
}