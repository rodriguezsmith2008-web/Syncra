package com.syncra.gestion_proyectos.repository.project;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncra.gestion_proyectos.entity.project.ExternalLinkEntity;

@Repository
public interface ExternalLinkRepository extends JpaRepository<ExternalLinkEntity, Long> {

    List<ExternalLinkEntity> findByProjectId(Long projectId);

}
