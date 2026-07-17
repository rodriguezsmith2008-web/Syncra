package com.syncra.gestion_proyectos.repository.task;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.task.TaskCommentEntity;

public interface TaskCommentRepository extends JpaRepository<TaskCommentEntity, Long> {

    /**
     * Obtiene los comentarios de una tarea, ordenados del mas antiguo al mas reciente
     *
     * @param taskId
     * @return lista de comentarios
     */
    List<TaskCommentEntity> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}