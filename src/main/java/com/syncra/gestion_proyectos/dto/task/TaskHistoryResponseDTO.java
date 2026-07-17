package com.syncra.gestion_proyectos.dto.task;

import java.time.LocalDateTime;

import lombok.Data;

/** DTO de respuesta con un registro del historial de una tarea */
@Data
public class TaskHistoryResponseDTO {

    private Long id;
    private Long taskId;
    private Long userId;
    private String action;
    private String fromValue;
    private String toValue;
    private LocalDateTime createdAt;
}