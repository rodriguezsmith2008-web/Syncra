package com.syncra.gestion_proyectos.entity.project;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Embeddable
@AllArgsConstructor
public class ProjectMemberId implements Serializable {

    private Long projectId;

    private Long userId;
}