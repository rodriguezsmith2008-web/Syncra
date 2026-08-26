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
    private Long userId;
    private LocalDateTime createdAt;

}