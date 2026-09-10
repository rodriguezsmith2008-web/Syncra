package com.syncra.gestion_proyectos.repository.ai;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.ai.AiProjectQuotaEntity;
import com.syncra.gestion_proyectos.enums.AiModelTierEnum;

public interface AiProjectQuotaRepository extends JpaRepository<AiProjectQuotaEntity, Long> {
    Optional<AiProjectQuotaEntity> findByProjectIdAndModelTier(Long projectId, AiModelTierEnum modelTier);
    List<AiProjectQuotaEntity> findByProjectId(Long projectId);
    List<AiProjectQuotaEntity> findByPeriodStartBefore(LocalDate periodStart);
}
