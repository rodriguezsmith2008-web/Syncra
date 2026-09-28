package com.syncra.gestion_proyectos.entity.activity;

import java.time.LocalDateTime;

import com.syncra.gestion_proyectos.enums.ActivityActionEnum;
import com.syncra.gestion_proyectos.enums.ActivityEntityTypeEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "activity_log")
public class ActivityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private ActivityEntityTypeEnum entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityActionEnum action;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "target_snippet", length = 255)
    private String targetSnippet;

    @Column(name = "target_section_key", length = 255)
    private String targetSectionKey;

    @Column(name = "change_kind", length = 32)
    private String changeKind;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}