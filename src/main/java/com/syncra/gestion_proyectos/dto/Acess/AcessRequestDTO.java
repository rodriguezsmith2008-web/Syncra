package com.syncra.gestion_proyectos.dto.Acess;

import java.sql.Date;

import lombok.Data;

@Data

public class AcessRequestDTO {

    private String firstName;

    private String lastName;

    private String email;

    private String documentNumber;

    private String groupName;
    
    private String status;

    private Date createdAt;
    
}
