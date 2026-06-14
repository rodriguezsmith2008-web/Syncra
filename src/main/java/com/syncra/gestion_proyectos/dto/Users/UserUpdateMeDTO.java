package com.syncra.gestion_proyectos.dto.Users;

import lombok.Data;

/**
 * DTO para que un usuario edite su propio perfil
 */
@Data
public class UserUpdateMeDTO {

    /**
     * Nombre del usuario
     */
    private String firstName;

    /**
     * Apellido del usuario
     */
    private String lastName;

    /**
     * Número de documento del usuario
     */
    private String documentNumber;

    /**
     * Correo electrónico del usuario
     */
    private String email;

    /**
     * URL de la foto de perfil
     */
    private String avatarUrl;

}