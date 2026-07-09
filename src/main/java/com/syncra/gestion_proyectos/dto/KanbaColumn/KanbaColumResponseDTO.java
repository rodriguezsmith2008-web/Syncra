package com.syncra.gestion_proyectos.dto.KanbaColumn;

import lombok.Data;

/** DTO de respuesta con la información de una columna kanban */
@Data
public class KanbaColumResponseDTO {

    private Long id;
    private Long projectId;
    private String name;
    private Long position;
    private String color;
}
