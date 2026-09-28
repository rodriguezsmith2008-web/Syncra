package com.syncra.gestion_proyectos.dto.access;

import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import lombok.Data;

@Data
public class AccessRequestDTO {

    private String firstName;

    private String lastName;

    private String email;

    private String documentNumber;

    private String groupName;

    private RoleUserEnum role;
}