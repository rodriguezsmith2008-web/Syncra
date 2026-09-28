package com.syncra.gestion_proyectos.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.syncra.gestion_proyectos.entity.ai.AiMessageEntity;

public interface AiMessageRepository extends JpaRepository<AiMessageEntity,Long>{
	List<AiMessageEntity> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
	void deleteByConversationId(Long conversationId);
}
