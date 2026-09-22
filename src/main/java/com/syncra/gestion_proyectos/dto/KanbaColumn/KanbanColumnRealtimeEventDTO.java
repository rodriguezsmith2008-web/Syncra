package com.syncra.gestion_proyectos.dto.KanbaColumn;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KanbanColumnRealtimeEventDTO {
    private Long projectId;
    private Long columnId;
    private String type;
}
