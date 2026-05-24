package com.syncra.gestion_proyectos.dto.Users;

import lombok.Data;

@Data
public class UserPasswordResetDTO {

private String resetToken;

private String newPassword;

}
