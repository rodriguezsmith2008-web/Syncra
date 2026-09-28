package com.syncra.gestion_proyectos.dto.ai;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AiConversationResponseDTO {
    private Long id;
    private Long projectId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}