package com.syncra.gestion_proyectos.repository.kanban;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.kanban.KanbanColumnEntity;

public interface KanbanColumnRepository extends JpaRepository<KanbanColumnEntity,Long> {

    List<KanbanColumnEntity> findByProjectIdOrderByPositionAsc(Long projectId);

    KanbanColumnEntity findByProjectIdAndNameIgnoreCase(Long projectId, String name);
}
