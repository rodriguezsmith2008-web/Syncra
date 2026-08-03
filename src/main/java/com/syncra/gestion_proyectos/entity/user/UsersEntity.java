package com.syncra.gestion_proyectos.entity.user;

import java.time.LocalDateTime;

import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.enums.UserStatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "users")
public class UsersEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "document_number", length = 20)
    private String documentNumber;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleUserEnum role = RoleUserEnum.APPRENTICE;

    @Column(name = "group_name", length = 20)
    private String groupName;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatusEnum status = UserStatusEnum.IN_TRAINING;

    @Column(name = "reset_token", length = 100)
    private String resetCode;

    @Column(name = "reset_token_expires")
    private LocalDateTime resetCodeExpires;

    // Fecha en que se creó la contraseña temporal (para saber si pasaron 24h)
    @Column(name = "temp_password_expires_at")
    private LocalDateTime tempPasswordExpiresAt;

    // Indica si el usuario debe cambiar la contraseña en el próximo login
    @Column(name = "must_change_password")
    private Boolean mustChangePassword = false;

}