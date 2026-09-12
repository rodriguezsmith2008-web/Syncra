package com.syncra.gestion_proyectos.dto.realtime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentPresenceEvent {

    private String type;
    private Long documentId;
    private Long userId;
    private String name;
    private String color;
    private String role;
}
