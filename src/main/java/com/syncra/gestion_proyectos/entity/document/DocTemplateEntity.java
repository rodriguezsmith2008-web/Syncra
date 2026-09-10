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
@Table(name = "doc_templates")
public class DocTemplateEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "position", nullable = false)
    private Long position = 0L;

    @Column(name = "default_content", columnDefinition = "LONGTEXT")
    private String defaultContent;

    @Column(name = "draft_content", columnDefinition = "LONGTEXT")
    private String draftContent;

    @Column(name = "draft_title", length = 200)
    private String draftTitle;

    @Column(name = "draft_description", length = 500)
    private String draftDescription;

    @Column(name = "published", nullable = false)
    private boolean published = true;

    @Column(name = "parent_template_id")
    private Long parentTemplateId;

}