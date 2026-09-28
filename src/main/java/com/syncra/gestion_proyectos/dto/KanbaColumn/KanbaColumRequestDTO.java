package com.syncra.gestion_proyectos.dto.KanbaColumn;

import lombok.Data;

/** DTO para crear o editar una columna kanban */
@Data
public class KanbaColumRequestDTO {
    private String name;
    private String color;
    private Long position;
    private Boolean isFinal;
}