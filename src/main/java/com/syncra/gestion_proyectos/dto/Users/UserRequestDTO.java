package com.syncra.gestion_proyectos.dto.users;

import lombok.Data;

@Data
public class UserRequestDTO {

    private String firstName;

    private String lastName;

    private String documentNumber;

    private String email;

    private String password;

    private String role;

    private String groupName;

    private String avatarUrl;

}
