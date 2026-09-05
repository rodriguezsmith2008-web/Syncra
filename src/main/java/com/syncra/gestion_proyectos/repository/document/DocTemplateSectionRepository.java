package com.syncra.gestion_proyectos.repository.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.document.DocTemplateSectionEntity;

public interface DocTemplateSectionRepository extends JpaRepository<DocTemplateSectionEntity, Long> {

    List<DocTemplateSectionEntity> findByTemplateIdOrderByPositionAsc(Long templateId);
}