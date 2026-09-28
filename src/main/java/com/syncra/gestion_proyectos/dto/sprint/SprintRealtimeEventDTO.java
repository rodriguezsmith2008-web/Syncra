package com.syncra.gestion_proyectos.dto.sprint;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SprintRealtimeEventDTO {
    private Long projectId;
    private Long sprintId;
    private String type;
}
