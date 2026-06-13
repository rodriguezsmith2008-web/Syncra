package com.syncra.gestion_proyectos.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.ai.AiMessageEntity;

public interface AiMessageRepository extends JpaRepository<AiMessageEntity,Long>{

    
}
