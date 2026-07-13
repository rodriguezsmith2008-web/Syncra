package com.syncra.gestion_proyectos.repository.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.task.TaskEntity;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    /**
     * Obtiene todas las tareas de un proyecto, ordenadas por columna y posicion
     *
     * @param projectId
     * @return lista de tareas del proyecto
     */
    List<TaskEntity> findByProjectIdOrderByColumnIdAscPositionAsc(Long projectId);

    /**
     * Obtiene las tareas de una columna especifica, ordenadas por posicion
     *
     * @param columnId
     * @return lista de tareas de la columna
     */
    List<TaskEntity> findByColumnIdOrderByPositionAsc(Long columnId);

    /**
     * Obtiene las tareas de un sprint especifico
     *
     * @param sprintId
     * @return lista de tareas del sprint
     */
    List<TaskEntity> findBySprintId(Long sprintId);

    /**
     * Obtiene las tareas asignadas a un usuario dentro de un proyecto
     *
     * @param projectId
     * @param assignedTo
     * @return lista de tareas asignadas
     */
    List<TaskEntity> findByProjectIdAndAssignedTo(Long projectId, Long assignedTo);

}