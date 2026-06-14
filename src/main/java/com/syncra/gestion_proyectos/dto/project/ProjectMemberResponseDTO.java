package com.syncra.gestion_proyectos.dto.project;

import lombok.Data;

@Data
public class ProjectMemberResponseDTO {

    /**
     * Dto de respuesta itilizado para representar la relación entre un proyecto y sus miembros
     */
    private Long projectId;
    private Long userId;

}
