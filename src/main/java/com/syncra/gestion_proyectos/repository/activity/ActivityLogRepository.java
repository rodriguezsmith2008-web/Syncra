package com.syncra.gestion_proyectos.repository.activity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.activity.ActivityLogEntity;

public interface ActivityLogRepository extends JpaRepository<ActivityLogEntity, Long> {

    /**
     * Obtiene todo el historial de actividad de un proyecto, más reciente primero
     *
     * @param projectId
     * @return lista de eventos de actividad
     */
    List<ActivityLogEntity> findByProjectIdOrderByCreatedAtDesc(Long projectId);

}