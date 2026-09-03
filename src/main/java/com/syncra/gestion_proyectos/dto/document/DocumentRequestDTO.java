package com.syncra.gestion_proyectos.dto.document;

import com.syncra.gestion_proyectos.enums.DocumentTypeEnum;

import lombok.Data;

/** DTO para crear un documento */
@Data
public class DocumentRequestDTO {

    private String title;
    private Long templateId;
    private Long parentDocumentId;
    private DocumentTypeEnum documentType;
    private Long sprintId;
    private Integer quarter;
    private String meetingType;
}