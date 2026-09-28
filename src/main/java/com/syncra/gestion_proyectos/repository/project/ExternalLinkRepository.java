package com.syncra.gestion_proyectos.repository.project;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.project.ExternalLinkEntity;

public interface ExternalLinkRepository extends JpaRepository<ExternalLinkEntity, Long> {

    //obtiene todos los enlaces externos asociados a un proyecto
    List<ExternalLinkEntity> findByProjectId(Long projectId);

}
