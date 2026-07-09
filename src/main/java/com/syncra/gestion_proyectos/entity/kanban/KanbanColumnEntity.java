package com.syncra.gestion_proyectos.entity.kanban;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "kanban_columns")
public class KanbanColumnEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "position", nullable = false)
    private Long position = 0L;

    @Column(name = "color", length = 20)
    private String color;

}
