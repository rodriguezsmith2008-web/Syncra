package com.syncra.gestion_proyectos.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO para que un usuario edite su propio perfil
 */
@Data
public class UserUpdateMeDTO {

    /**
     * Nombre del usuario
     */
    @NotBlank(message = "El nombre es obligatorio.")
    private String firstName;

    /**
     * Apellido del usuario
     */
    @NotBlank(message = "El apellido es obligatorio.")
    private String lastName;

    /**
     * Número de documento del usuario
     */
    @Pattern(regexp = "^[0-9A-Za-z-]+$", message = "El documento solo puede contener números, letras y guiones.")
    private String documentNumber;

    /**
     * Correo electrónico del usuario
     */
    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Ingresa un correo electrónico válido.")
    private String email;

    /**
     * URL de la foto de perfil
     */
    private String avatarUrl;

    private String avatarPublicId;

}