package com.syncra.gestion_proyectos.dto.users;

import lombok.Data;

@Data
public class UserUpdateDTO {

    private String firstName;

    private String lastName;

    private String documentNumber;

    private String groupName;

    private String avatarUrl;

    private String avatarPublicId;

    private String status;

    private String role;

}