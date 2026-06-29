package com.syncra.gestion_proyectos.dto.users;

import lombok.Data;

/**
 * DTO para cambiar la contraseña del usuario autenticado
 */
@Data
public class UserChangePasswordDTO {

    /** Contraseña actual */
    private String currentPassword;

    /** Nueva contraseña */
    private String newPassword;
}