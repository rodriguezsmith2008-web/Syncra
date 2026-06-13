package com.syncra.gestion_proyectos.entity.project;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "external_links")
public class ExternalLinkEntity {


    /**
     * Id único del enlace externo
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Id del proyecto al que pertenece el enlace
     */

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * Titulo o nombre descriptivo del enlace
     */

    @Column(name = "title", nullable = false, length =  200)
    private String title;

    /**
     * Dirección URL del recurso externo
     */

    @Column(name = "url", nullable = false, length = 1000)
    private String url;

    /**
     * Identificador del usuario que registró el enlace
     */

    @Column(name = "added_by", nullable = false)
    private Long addedBy;

    /**
     * Fecha y hora de la creación del registro
     */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Este método se ejecuta antes de insertar el registro a la base de datos y asigna automaticamente la fecha y hora actual en el campo
     */

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }
    
}
