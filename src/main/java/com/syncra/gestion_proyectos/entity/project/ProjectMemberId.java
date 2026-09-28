package com.syncra.gestion_proyectos.entity.project;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMemberId implements Serializable {

    // id del proyecto
    private Long projectId;

    // id del usuario miembro del proyecto
    private Long userId;
}