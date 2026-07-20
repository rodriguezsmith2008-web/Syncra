package com.syncra.gestion_proyectos.dto.document;

import com.syncra.gestion_proyectos.enums.DocumentStatusEnum;

import lombok.Data;

/** DTO usado por el instructor para aprobar o rechazar un documento */
@Data
public class DocumentStatusUpdateDTO {

    private DocumentStatusEnum status;
}