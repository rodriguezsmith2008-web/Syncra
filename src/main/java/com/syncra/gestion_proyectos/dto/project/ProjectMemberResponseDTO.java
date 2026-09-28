package com.syncra.gestion_proyectos.dto.project;

import lombok.Data;

@Data
public class ProjectMemberResponseDTO {

    private Long projectId;
    private Long userId;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String email;
    private String documentNumber;
    private String role;

}