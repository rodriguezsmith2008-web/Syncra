package com.syncra.gestion_proyectos.repository.document;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.document.DocumentCommentEntity;

public interface DocumentCommentRepository extends JpaRepository<DocumentCommentEntity, Long> {

    /** Obtiene todos los comentarios de un documento */
    List<DocumentCommentEntity> findByDocumentId(Long documentId);
}