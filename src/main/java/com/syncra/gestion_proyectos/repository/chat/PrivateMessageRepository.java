package com.syncra.gestion_proyectos.repository.chat;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.chat.PrivateMessageEntity;

public interface PrivateMessageRepository extends JpaRepository<PrivateMessageEntity, Long> {

    List<PrivateMessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
