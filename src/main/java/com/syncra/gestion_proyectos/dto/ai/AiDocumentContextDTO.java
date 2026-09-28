package com.syncra.gestion_proyectos.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiDocumentContextDTO {
    private Long id;
    private String title;
    private String type;
    private Long parentDocumentId;
    private String status;
    private String content;
    private String meetingType;
}