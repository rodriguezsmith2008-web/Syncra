package com.syncra.gestion_proyectos.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.syncra.gestion_proyectos.entity.ai.AiConversationEntity;

public interface AiConversationRepository extends JpaRepository<AiConversationEntity,Long>{
	List<AiConversationEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
	java.util.Optional<AiConversationEntity> findByIdAndUserId(Long id, Long userId);
} 