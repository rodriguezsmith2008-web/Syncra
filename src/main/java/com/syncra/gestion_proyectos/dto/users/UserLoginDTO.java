package com.syncra.gestion_proyectos.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserLoginDTO {

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "Ingresa un correo electrónico válido.")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;

}
