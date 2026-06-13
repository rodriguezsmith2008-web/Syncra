package com.syncra.gestion_proyectos.repository.project;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncra.gestion_proyectos.entity.project.ProjectEntity;
import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    List<ProjectEntity> findByCreatedBy(Long createdBy);

    List<ProjectEntity> findByStatus(ProjectStatusEnum status);

    List<ProjectEntity> findByGroupName(String groupName);
}
