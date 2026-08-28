package com.syncra.gestion_proyectos.dto.chat;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ConversationResponseDTO {

    private Long id;
    private Long projectId;
    private Long userOneId;
    private Long userTwoId;
    private LocalDateTime createdAt;
}
