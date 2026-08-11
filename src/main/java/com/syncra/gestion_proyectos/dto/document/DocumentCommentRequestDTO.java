package com.syncra.gestion_proyectos.dto.document;

import lombok.Data;

/** DTO para agregar un comentario a un documento */
@Data
public class DocumentCommentRequestDTO {

    private String content;
    private Long parentCommentId; 
}