package com.syncra.gestion_proyectos.dto.notifications;

import java.time.LocalDateTime;

import lombok.Data;

/** DTO de respuesta con la información de una notificación */
@Data
public class NotificationResponseDTO {

    private Long id;
    private Long userId;
    private Long taskId;
    private String type;
    private Long projectId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private Long documentId;
}