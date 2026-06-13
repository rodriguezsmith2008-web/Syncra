package com.syncra.gestion_proyectos.dto.users;

import lombok.Data;

@Data
public class UserPasswordResetDTO {

private String resetToken;

private String newPassword;

}
