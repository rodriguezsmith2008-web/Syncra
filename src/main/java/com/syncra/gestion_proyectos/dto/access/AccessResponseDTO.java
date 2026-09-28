package com.syncra.gestion_proyectos.dto.access;

import lombok.Data;

@Data
public class AccessResponseDTO {
    
    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String documentNumber;

    private String groupName;

    private String role;   

    private String status;

    private String createdAt;

}
