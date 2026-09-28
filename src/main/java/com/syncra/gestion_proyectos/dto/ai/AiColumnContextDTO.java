package com.syncra.gestion_proyectos.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiColumnContextDTO {
    private Long id;
    private String name;
    private Long position;
}