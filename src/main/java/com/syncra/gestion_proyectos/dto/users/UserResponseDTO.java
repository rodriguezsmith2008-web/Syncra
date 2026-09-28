package com.syncra.gestion_proyectos.dto.users;

import lombok.Data;

@Data
public class UserResponseDTO {

    private Long id;

    private String firstName;

    private String lastName;

    private String documentNumber;

    private String email;

    private String role;

    private String groupName;

    private String avatarUrl;

    private String status;

    private Boolean hasProject;

    private Boolean hasSeenOnboarding;

}
