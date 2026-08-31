package com.syncra.gestion_proyectos.dto.passwordreset;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;


//Guarda la nueva contraseña 
@Data
public class ResetPasswordDTO {
    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Ingresa un correo electrónico válido.")
    private String email;

    @NotBlank(message = "El código de verificación es obligatorio.")
    @Pattern(regexp = "\\d{6}", message = "El código debe contener 6 dígitos.")
    private String code;

    @NotBlank(message = "La nueva contraseña es obligatoria.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'\\|,.<>/?~`]).{8,}$",
        message = "La contraseña debe tener al menos 8 caracteres, un número y un carácter especial."
    )
    private String newPassword;
}