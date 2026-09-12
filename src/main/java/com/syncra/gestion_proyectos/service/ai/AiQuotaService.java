package com.syncra.gestion_proyectos.service.ai;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.syncra.gestion_proyectos.dto.ai.AiQuotaStatusDTO;
import com.syncra.gestion_proyectos.entity.ai.AiProjectQuotaEntity;
import com.syncra.gestion_proyectos.enums.AiModelTierEnum;
import com.syncra.gestion_proyectos.repository.ai.AiProjectQuotaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuotaService {
    private final AiProjectQuotaRepository repo;

    private static final Map<AiModelTierEnum, Integer> REQUEST_LIMITS = new EnumMap<>(AiModelTierEnum.class);
    private static final Map<AiModelTierEnum, Long> TOKEN_LIMITS = new EnumMap<>(AiModelTierEnum.class);
    private static final Map<AiModelTierEnum, Integer> MAX_OUTPUT_TOKENS = new EnumMap<>(AiModelTierEnum.class);

    static {
        REQUEST_LIMITS.put(AiModelTierEnum.PRIMARY, 10);
        REQUEST_LIMITS.put(AiModelTierEnum.SECONDARY, 15);
        REQUEST_LIMITS.put(AiModelTierEnum.FALLBACK, 20);

        TOKEN_LIMITS.put(AiModelTierEnum.PRIMARY, 30_000L);
        TOKEN_LIMITS.put(AiModelTierEnum.SECONDARY, 30_000L);
        TOKEN_LIMITS.put(AiModelTierEnum.FALLBACK, 20_000L);

        MAX_OUTPUT_TOKENS.put(AiModelTierEnum.PRIMARY, 4_096);
        MAX_OUTPUT_TOKENS.put(AiModelTierEnum.SECONDARY, 3_072);
        MAX_OUTPUT_TOKENS.put(AiModelTierEnum.FALLBACK, 2_048);
    }

    @Transactional
    public synchronized AiProjectQuotaEntity getOrCreateQuota(Long projectId, AiModelTierEnum tier) {
        if (projectId == null) {
            throw new IllegalArgumentException("El proyecto es obligatorio para aplicar cuotas");
        }

        LocalDate today = LocalDate.now();
        Optional<AiProjectQuotaEntity> existing = repo.findByProjectIdAndModelTier(projectId, tier);
        if (existing.isPresent()) {
            AiProjectQuotaEntity quota = existing.get();
            if (!today.equals(quota.getPeriodStart())) {
                quota.setPeriodStart(today);
                quota.setRequestsUsed(0);
                quota.setTokensUsed(0L);
            }
            if (quota.getRequestsLimit() <= 0) {
                quota.setRequestsLimit(REQUEST_LIMITS.getOrDefault(tier, 10));
                quota.setTokensLimit(TOKEN_LIMITS.getOrDefault(tier, 30_000L));
                quota.setMaxOutputTokens(MAX_OUTPUT_TOKENS.getOrDefault(tier, 2000));
            }
            return repo.save(quota);
        }

        AiProjectQuotaEntity quota = new AiProjectQuotaEntity();
        quota.setProjectId(projectId);
        quota.setModelTier(tier);
        quota.setPeriodStart(today);
        quota.setRequestsLimit(REQUEST_LIMITS.getOrDefault(tier, 10));
        quota.setTokensLimit(TOKEN_LIMITS.getOrDefault(tier, 30_000L));
        quota.setMaxOutputTokens(MAX_OUTPUT_TOKENS.getOrDefault(tier, 2000));
        return repo.save(quota);
    }

    /**
     * Decide si la tier está disponible AHORA MISMO.
     * La acción en curso siempre se completa (no se corta a mitad).
     * Cuando el límite se supera, la siguiente petición ya cae a la tier inferior.
     */
    @Transactional
    public synchronized boolean canUse(Long projectId, AiModelTierEnum tier, long requestedTokens) {
        AiProjectQuotaEntity quota = getOrCreateQuota(projectId, tier);
        if (!LocalDate.now().equals(quota.getPeriodStart())) {
            quota.setPeriodStart(LocalDate.now());
            quota.setRequestsUsed(0);
            quota.setTokensUsed(0L);
            quota = repo.save(quota);
        }
        return quota.getRequestsUsed() < quota.getRequestsLimit()
                && quota.getTokensUsed() < quota.getTokensLimit();
    }

    @Transactional
    public synchronized AiProjectQuotaEntity consume(Long projectId, AiModelTierEnum tier, long tokensConsumed) {
        AiProjectQuotaEntity quota = getOrCreateQuota(projectId, tier);
        if (!LocalDate.now().equals(quota.getPeriodStart())) {
            quota.setPeriodStart(LocalDate.now());
            quota.setRequestsUsed(0);
            quota.setTokensUsed(0L);
        }

        quota.setRequestsUsed(quota.getRequestsUsed() + 1);
        quota.setTokensUsed(quota.getTokensUsed() + Math.max(tokensConsumed, 0L));
        return repo.save(quota);
    }

    @Transactional
    public synchronized void resetExpiredPeriods() {
        LocalDate today = LocalDate.now();
        List<AiProjectQuotaEntity> expired = repo.findByPeriodStartBefore(today);
        for (AiProjectQuotaEntity quota : expired) {
            quota.setPeriodStart(today);
            quota.setRequestsUsed(0);
            quota.setTokensUsed(0L);
        }
        if (!expired.isEmpty()) {
            repo.saveAll(expired);
        }
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void scheduledReset() {
        resetExpiredPeriods();
        log.info("Reset diario de cuotas IA ejecutado");
    }

    public AiQuotaStatusDTO toStatus(AiProjectQuotaEntity quota) {
        AiQuotaStatusDTO dto = new AiQuotaStatusDTO();
        dto.setRequestsUsed(quota.getRequestsUsed());
        dto.setRequestsLimit(quota.getRequestsLimit());
        dto.setRequestsRemaining(Math.max(0, quota.getRequestsLimit() - quota.getRequestsUsed()));
        dto.setTokensUsed(quota.getTokensUsed());
        dto.setTokensLimit(quota.getTokensLimit());
        dto.setTokensRemaining(Math.max(0L, quota.getTokensLimit() - quota.getTokensUsed()));
        return dto;
    }

    public List<AiProjectQuotaEntity> getProjectQuotas(Long projectId) {
        return repo.findByProjectId(projectId);
    }

    public Map<AiModelTierEnum, AiProjectQuotaEntity> getCurrentQuotaMap(Long projectId) {
        List<AiProjectQuotaEntity> quotas = repo.findByProjectId(projectId);
        Map<AiModelTierEnum, AiProjectQuotaEntity> map = new EnumMap<>(AiModelTierEnum.class);
        for (AiProjectQuotaEntity quota : quotas) {
            if (LocalDate.now().equals(quota.getPeriodStart())) {
                map.put(quota.getModelTier(), quota);
            }
        }
        return map;
    }

    public static List<AiModelTierEnum> modelPriority() {
        return new ArrayList<>(List.of(AiModelTierEnum.PRIMARY, AiModelTierEnum.SECONDARY, AiModelTierEnum.FALLBACK));
    }

    public AiModelTierEnum nextAvailableTier(Long projectId, AiModelTierEnum currentTier) {
        for (AiModelTierEnum tier : modelPriority()) {
            if (tier == currentTier) continue;
            if (canUse(projectId, tier, 0L)) return tier;
        }
        return null;
    }

    public String statusMessageForTier(AiModelTierEnum tier) {
        if (tier == AiModelTierEnum.SECONDARY) {
            return "El límite del modelo avanzado se alcanzó para este proyecto. Syncra AI continuará utilizando un modelo estándar.";
        }
        if (tier == AiModelTierEnum.FALLBACK) {
            return "El límite del modelo estándar se alcanzó. Syncra AI continuará en modo básico.";
        }
        return "Estas usando el modo avanzado de Syncra AI.";
    }
}