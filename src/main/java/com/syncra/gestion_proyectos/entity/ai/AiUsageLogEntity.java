package com.syncra.gestion_proyectos.entity.ai;

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
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(
    name = "ai_usage_logs",
    indexes = {
        @Index(name = "idx_ai_usage_logs_project_date", columnList = "project_id, created_at"),
        @Index(name = "idx_ai_usage_logs_user_date", columnList = "user_id, created_at")
    }
)
public class AiUsageLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_tier", nullable = false)
    private AiModelTierEnum modelTier;

    @Column(name = "model_name", nullable = false, length = 200)
    private String modelName;

    @Column(name = "request_tokens")
    private Long requestTokens;

    @Column(name = "response_tokens")
    private Long responseTokens;

    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "result", length = 50)
    private String result;

    @Column(name = "fallback_used", nullable = false)
    private boolean fallbackUsed = false;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
