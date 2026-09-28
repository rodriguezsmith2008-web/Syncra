package com.syncra.gestion_proyectos.entity.document;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "doc_template_sections")
public class DocTemplateSectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "section_key", nullable = false, length = 50)
    private String sectionKey;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "default_content", columnDefinition = "LONGTEXT")
    private String defaultContent;

    @Column(name = "position", nullable = false)
    private Integer position = 0;

}