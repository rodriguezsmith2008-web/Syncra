package com.syncra.gestion_proyectos.dto.document;

import lombok.Data;

/** DTO para actualizar el contenido de un documento */
@Data
public class DocumentUpdateDTO {

    private String title;
    private String content;
}