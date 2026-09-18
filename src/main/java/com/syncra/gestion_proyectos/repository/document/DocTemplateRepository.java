package com.syncra.gestion_proyectos.repository.document;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.syncra.gestion_proyectos.entity.document.DocTemplateEntity;

public interface DocTemplateRepository extends JpaRepository<DocTemplateEntity, Long> {

    Optional<DocTemplateEntity> findByCode(String code);

    List<DocTemplateEntity> findAllByOrderByPositionAsc();

    List<DocTemplateEntity> findAllByPublishedTrueOrderByPositionAsc();

    List<DocTemplateEntity> findAllByPublishedTrueAndParentTemplateIdIsNullOrderByPositionAsc();

    List<DocTemplateEntity> findByParentTemplateIdIsNullAndPublishedTrueOrderByPositionAsc();

    List<DocTemplateEntity> findByParentTemplateIdOrderByPositionAsc(Long parentTemplateId);

    @Query("SELECT t FROM DocTemplateEntity t WHERE t.parentTemplateId IS NULL AND t.published = true ORDER BY t.position ASC")
    List<DocTemplateEntity> findRootPublishedTemplates();
}