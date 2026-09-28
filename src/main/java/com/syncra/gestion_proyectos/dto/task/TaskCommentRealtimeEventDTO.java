package com.syncra.gestion_proyectos.dto.task;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskCommentRealtimeEventDTO {
    private Long taskId;
    private Long commentId;
    private String action;
    private TaskCommentResponseDTO comment;
}
