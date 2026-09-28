package com.syncra.gestion_proyectos.dto.project;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProjectMemberRealtimeEventDTO {
    private Long projectId;
    private Long userId;
    private String action;
    private ProjectMemberResponseDTO member;
}
