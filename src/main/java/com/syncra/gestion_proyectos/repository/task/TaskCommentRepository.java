package com.syncra.gestion_proyectos.repository.task;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.task.TaskCommentEntity;

public interface TaskCommentRepository extends JpaRepository<TaskCommentEntity,Long> {

    
}
