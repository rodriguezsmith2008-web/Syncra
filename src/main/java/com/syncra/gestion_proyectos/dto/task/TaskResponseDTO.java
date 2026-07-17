package com.syncra.gestion_proyectos.dto.task;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/** DTO de respuesta con la información de una tarea */
@Data
public class TaskResponseDTO {

    private Long id;
    private Long projectId;
    private Long columnId;
    private Long sprintId;
    private String title;
    private String description;
    private String color;
    private LocalDate dueDate;
    private Long assignedTo;
    private Long createdBy;
    private Long position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}