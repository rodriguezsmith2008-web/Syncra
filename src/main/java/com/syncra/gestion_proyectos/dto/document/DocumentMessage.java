package com.syncra.gestion_proyectos.dto.document;

import lombok.Data;

/** DTO para mensajes de respuesta de documentos */
@Data
public class DocumentMessage {

    private String message;
    private DocumentCommentResponseDTO comment;
}