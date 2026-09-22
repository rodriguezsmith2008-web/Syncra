package com.syncra.gestion_proyectos.dto.chat;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ChatMessageResponseDTO {

    private Long id;
    private Long projectId;
    private Long conversationId;
    private Long userId;
    private Long senderId;
    private String senderFullName;
    private String senderAvatarUrl;
    private String content;
    private Boolean isRead;
    private String type;
    private LocalDateTime createdAt;
}
