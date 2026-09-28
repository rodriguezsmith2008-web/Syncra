package com.syncra.gestion_proyectos.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRealtimeEventDTO {

    private Long projectId;
    private Long conversationId;
    private String type;
}