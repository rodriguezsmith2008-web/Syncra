package com.syncra.gestion_proyectos.entity.access;

import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "access_requests")

public class AcessRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "document_number", nullable = false)
    private String documentNumber;
    @Column(name = "group_name", nullable = false)
    private String groupName;
    @Column(name = "status", nullable = false)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Date createdAt;

    
    
}