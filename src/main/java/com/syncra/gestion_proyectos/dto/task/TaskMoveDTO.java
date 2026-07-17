package com.syncra.gestion_proyectos.dto.task;

import lombok.Data;

/** DTO usado exclusivamente para mover una tarea de columna */
@Data
public class TaskMoveDTO {

    private Long columnId;
    private Long position;
}