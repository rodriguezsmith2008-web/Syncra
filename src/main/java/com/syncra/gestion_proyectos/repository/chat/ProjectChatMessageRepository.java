package com.syncra.gestion_proyectos.repository.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.chat.ProjectChatMessageEntity;

public interface ProjectChatMessageRepository extends JpaRepository<ProjectChatMessageEntity, Long> {

    List<ProjectChatMessageEntity> findByProjectIdOrderByCreatedAtAsc(Long projectId);
}
