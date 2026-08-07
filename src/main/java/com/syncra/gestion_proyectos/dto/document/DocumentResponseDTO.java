package com.syncra.gestion_proyectos.dto.document;

import java.time.LocalDateTime;

import com.syncra.gestion_proyectos.enums.DocumentStatusEnum;
import com.syncra.gestion_proyectos.enums.DocumentTypeEnum;

import lombok.Data;

/** DTO de respuesta con la información de un documento */
@Data
public class DocumentResponseDTO {

    private Long id;
    private Long projectId;
    private Long templateId;
    private String title;
    private String content;
    private DocumentStatusEnum status;
    private DocumentTypeEnum documentType;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long parentDocumentId;
    private Integer sortOrder;
}