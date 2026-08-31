package com.syncra.gestion_proyectos.dto.users;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para cambiar la contraseña del usuario autenticado
 */
@Data
public class UserChangePasswordDTO {

    /** Contraseña actual */
    @NotBlank(message = "Debes ingresar tu contraseña actual.")
    private String currentPassword;

    /** Nueva contraseña */
    @NotBlank(message = "La nueva contraseña es obligatoria.")
    @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres.")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'\\|,.<>/?~`]).{8,}$",
        message = "La nueva contraseña debe incluir un número y un carácter especial."
    )
    private String newPassword;
}