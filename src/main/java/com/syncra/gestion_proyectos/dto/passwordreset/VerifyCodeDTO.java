package com.syncra.gestion_proyectos.dto.passwordreset;

import lombok.Data;


//verifica que el codigo sea correcto
@Data
public class VerifyCodeDTO {
    private String email;
    private String code;
}