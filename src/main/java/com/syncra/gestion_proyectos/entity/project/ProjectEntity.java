package com.syncra.gestion_proyectos.entity.project;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.syncra.gestion_proyectos.enums.ProjectStatusEnum;

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
@Table(name = "projects")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "group_name", length = 20)
    private String groupName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatusEnum status = ProjectStatusEnum.IN_PROGRESS;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }
}
