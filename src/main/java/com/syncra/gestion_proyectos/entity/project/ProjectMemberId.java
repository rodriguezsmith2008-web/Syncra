package com.syncra.gestion_proyectos.entity.project;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class ProjectMemberId implements Serializable {

    private Long projectId;

    private Long userId;
}