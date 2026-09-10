package com.syncra.gestion_proyectos.dto.document;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocTemplateRealtimeEventDTO {

    private Long templateId;
    private String type;
}