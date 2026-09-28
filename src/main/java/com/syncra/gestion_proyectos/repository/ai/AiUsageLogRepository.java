package com.syncra.gestion_proyectos.repository.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.ai.AiUsageLogEntity;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLogEntity, Long> {
}
