package com.syncra.gestion_proyectos.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiHistoryItemDTO {
    private String role;
    private String content;
}