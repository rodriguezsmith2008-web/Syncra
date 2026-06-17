package com.syncra.gestion_proyectos.entity.access;

import java.time.LocalDateTime;

import com.syncra.gestion_proyectos.enums.AccessStatusEnum;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "access_requests")
public class AccessRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "document_number")
    private String documentNumber;

    @Column(name = "group_name")
    private String groupName;



    // guarda pendiente en la base de datos
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RoleUserEnum role = RoleUserEnum.APPRENTICE;

    // por defecto pone el estado como pendiente
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccessStatusEnum status = AccessStatusEnum.PENDING;

    // La fecha se asigna automaticamente en java, sin preguntarle al usuario
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}