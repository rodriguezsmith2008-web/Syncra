package com.syncra.gestion_proyectos.dto.passwordreset;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


//verifica que el codigo sea correcto
@Data
public class VerifyCodeDTO {
    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Ingresa un correo electrónico válido.")
    private String email;

    @NotBlank(message = "El código de verificación es obligatorio.")
    @Pattern(regexp = "\\d{6}", message = "El código debe contener 6 dígitos.")
    private String code;
}