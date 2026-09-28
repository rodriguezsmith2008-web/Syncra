package com.syncra.gestion_proyectos.repository.sprint;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.sprint.SprintEntity;
import com.syncra.gestion_proyectos.enums.SprintStatusEnum;

public interface SprintRepository extends JpaRepository<SprintEntity, Long> {
    
    /** obtiene todos los sprints de un proyecto */
    List<SprintEntity> findByProjectId(Long projectId);

    /** obtiene los sprints de un proyecto filtrados por estado */
    List<SprintEntity> findByProjectIdAndStatus(Long projectId, SprintStatusEnum status);
}
