package com.syncra.gestion_proyectos.dto.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatMessageRequestDTO {

    @NotBlank
    private String content;

    private Long replyToMessageId;
}
