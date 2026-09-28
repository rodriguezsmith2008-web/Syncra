package com.syncra.gestion_proyectos.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleCountDTO {

    private String role;
    private Long count;

}
