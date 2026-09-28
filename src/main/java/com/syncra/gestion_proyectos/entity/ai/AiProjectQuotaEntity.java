package com.syncra.gestion_proyectos.entity.ai;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.syncra.gestion_proyectos.enums.AiModelTierEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(
    name = "ai_project_quotas",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "model_tier"}),
    indexes = @Index(name = "idx_ai_project_quotas_project_period", columnList = "project_id, period_start")
)
public class AiProjectQuotaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_tier", nullable = false)
    private AiModelTierEnum modelTier;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "requests_used", nullable = false)
    private int requestsUsed = 0;

    @Column(name = "tokens_used", nullable = false)
    private long tokensUsed = 0L;

    @Column(name = "requests_limit", nullable = false)
    private int requestsLimit;

    @Column(name = "tokens_limit", nullable = false)
    private long tokensLimit;

    @Column(name = "max_output_tokens", nullable = false)
    private int maxOutputTokens;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.periodStart == null) {
            this.periodStart = LocalDate.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
