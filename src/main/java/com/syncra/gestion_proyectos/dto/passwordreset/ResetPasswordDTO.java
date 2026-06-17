package com.syncra.gestion_proyectos.dto.passwordreset;

import lombok.Data;


//Guarda la nueva contraseña 
@Data
public class ResetPasswordDTO {
    private String email;
    private String code;
    private String newPassword;
}