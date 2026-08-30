package com.syncra.gestion_proyectos.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRequestDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio.")
    private String lastName;

    @Pattern(regexp = "^[0-9A-Za-z-]+$", message = "El documento solo puede contener números, letras y guiones.")
    private String documentNumber;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Ingresa un correo electrónico válido.")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres.")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[!@#$%^&*()_+=\\-{}\\[\\]:;\"'\\|,.<>/?~`]).{8,}$",
        message = "La contraseña debe contener al menos un número y un carácter especial."
    )
    private String password;

    private String role;

    private String groupName;

    private String avatarUrl;

    private String status;

}
