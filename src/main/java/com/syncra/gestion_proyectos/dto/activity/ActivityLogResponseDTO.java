package com.syncra.gestion_proyectos.dto.activity;

import java.time.LocalDateTime;

import com.syncra.gestion_proyectos.enums.ActivityActionEnum;
import com.syncra.gestion_proyectos.enums.ActivityEntityTypeEnum;

import lombok.Data;

/** DTO de respuesta con la información de un evento de actividad */
@Data
public class ActivityLogResponseDTO {

    private Long id;
    private Long projectId;
    private ActivityEntityTypeEnum entityType;
    private Long entityId;
    private ActivityActionEnum action;
    private String description;
    private String targetSnippet;
    private String targetSectionKey;
    private String changeKind;
    private Long userId;
    private String userFullName;
    private String userAvatarUrl;
    private LocalDateTime createdAt;

}