package com.syncra.gestion_proyectos.entity.project;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "project_members")
public class ProjectMemberEntity {

    @EmbeddedId
    private ProjectMemberId id;

}