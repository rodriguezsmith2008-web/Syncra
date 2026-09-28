package com.syncra.gestion_proyectos.repository.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.task.TaskHistoryEntity;

public interface TaskHistoryRepository extends JpaRepository<TaskHistoryEntity, Long> {

    /**
     * Obtiene el historial de una tarea, del cambio mas reciente al mas antiguo
     *
     * @param taskId
     * @return lista de registros de historial
     */
    List<TaskHistoryEntity> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<TaskHistoryEntity> findByTaskIdInOrderByCreatedAtDesc(List<Long> taskIds);
}