package com.syncra.gestion_proyectos.dto.task;

import lombok.Data;

/** DTO para agregar un comentario a una tarea */
@Data
public class TaskCommentRequestDTO {

    private String content;
}