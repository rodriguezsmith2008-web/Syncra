package com.syncra.gestion_proyectos.dto.task;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TaskRealtimeEventDTO {
    private Long projectId;
    private Long taskId;
    private String type;
}
