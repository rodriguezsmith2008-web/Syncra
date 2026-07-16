package com.syncra.gestion_proyectos.dto.task;

import java.time.LocalDateTime;

import lombok.Data;

/** DTO de respuesta con la información de un comentario de tarea */
@Data
public class TaskCommentResponseDTO {

    private Long id;
    private Long taskId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
}