package com.syncra.gestion_proyectos.entity.project;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "project_members")
public class ProjectMemberEntity {

    /**
     * Clave primaria compuesta que contiene los identificadores del proyecto y del usuario asociado
     */
    @EmbeddedId
    private ProjectMemberId id;

}