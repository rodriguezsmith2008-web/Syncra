package com.syncra.gestion_proyectos.dto.ai;

import lombok.Data;

@Data
public class AiConversationRequestDTO {
    private Long projectId;
    private String title;
}