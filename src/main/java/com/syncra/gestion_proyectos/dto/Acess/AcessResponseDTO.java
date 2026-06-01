package com.syncra.gestion_proyectos.dto.Acess;

import lombok.Data;

@Data
public class AcessResponseDTO {
    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String documentNumber;

    private String groupName;

    private String status;

    private String createdAt;

}
