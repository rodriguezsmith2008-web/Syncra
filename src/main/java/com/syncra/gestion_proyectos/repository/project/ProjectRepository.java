package com.syncra.gestion_proyectos.repository.project;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.project.ProjectEntity;
import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;

public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    //obtiene todos los proyectos creados por el usuario
    List<ProjectEntity> findByCreatedBy(Long createdBy);

    //obtiene todos los proyectos que se encuentran en un estado específico
    List<ProjectEntity> findByStatus(ProjectStatusEnum status);

    //obtiene los proyectos que se encuentren a un grupo específico
    List<ProjectEntity> findByGroupName(String groupName);
}
