package com.syncra.gestion_proyectos.repository.task;

import java.util.List;
import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.task.TaskEntity;

public interface TaskRepository extends JpaRepository<TaskEntity, Long> {

    List<TaskEntity> findByDueDate(LocalDate dueDate);

    /**
     * Obtiene todas las tareas de un proyecto, ordenadas por columna y posicion.
     * Es la consulta que arma el tablero kanban completo.
     *
     * @param projectId
     * @return lista de tareas del proyecto
     */
    List<TaskEntity> findByProjectIdOrderByColumnIdAscPositionAsc(Long projectId);

    /**
     * Obtiene las tareas de una columna especifica, ordenadas por posicion.
     *
     * @param columnId
     * @return lista de tareas de la columna
     */
    List<TaskEntity> findByColumnIdOrderByPositionAsc(Long columnId);

    /**
     * Obtiene las tareas asociadas a un sprint especifico.
     *
     * @param sprintId
     * @return lista de tareas del sprint
     */
    List<TaskEntity> findBySprintId(Long sprintId);

    /**
     * Obtiene las tareas de un proyecto asignadas a un usuario especifico.
     *
     * @param projectId
     * @param assignedTo
     * @return lista de tareas asignadas al usuario
     */
    List<TaskEntity> findByProjectIdAndAssignedTo(Long projectId, Long assignedTo);

    /**
     * Obtiene las tareas de una columna que estan despues de una posicion dada.
     * Se usa para reindexar posiciones cuando una tarea se mueve o se elimina,
     * asi no quedan huecos en el orden de la columna.
     *
     * @param columnId
     * @param position
     * @return lista de tareas afectadas
     */
    List<TaskEntity> findByColumnIdAndPositionGreaterThan(Long columnId, Long position);

    List<TaskEntity> findByColumnIdAndPositionGreaterThanEqual(Long columnId, Long position);

    List<TaskEntity> findByColumnIdAndPositionBetween(Long columnId, Long start, Long end);
}