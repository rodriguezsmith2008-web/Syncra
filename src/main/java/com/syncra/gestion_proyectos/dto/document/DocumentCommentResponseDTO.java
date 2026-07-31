package com.syncra.gestion_proyectos.dto.document;

import java.time.LocalDateTime;

import lombok.Data;

/** DTO de respuesta con la información de un comentario */
@Data
public class DocumentCommentResponseDTO {

    private Long id;
    private Long documentId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;

    // Datos del usuario que comentó
    private String userFullName;
    private String userAvatarUrl;

    // Tiempo formateado: "hace 5 minutos" o "15 jun 2025" si pasaron +24h
    private String timeDisplay;
}