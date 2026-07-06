package com.syncra.gestion_proyectos.repository.document;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.document.DocTemplateEntity;

public interface DocTemplateRepository extends JpaRepository<DocTemplateEntity,Long>{

    Optional<DocTemplateEntity> findByCode(String code);
} 