package com.syncra.gestion_proyectos.dto.users;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyCurrentPasswordDTO {

    @NotBlank(message = "Debes ingresar tu contraseña actual.")
    private String currentPassword;
}
