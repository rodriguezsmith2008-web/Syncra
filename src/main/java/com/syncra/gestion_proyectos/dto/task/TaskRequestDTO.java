package com.syncra.gestion_proyectos.dto.task;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.Data;

/** DTO para crear o editar una tarea */
@Data
public class TaskRequestDTO {

    private Long columnId;
    private Long sprintId;
    private String title;
    private String description;
    private String color;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Long assignedTo;
    private Long position;
}