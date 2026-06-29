package com.syncra.gestion_proyectos.dto.document;

import lombok.Data;

/** DTO para crear un documento */
@Data
public class DocumentRequestDTO {

    private String title;
    private Long templateId;
}