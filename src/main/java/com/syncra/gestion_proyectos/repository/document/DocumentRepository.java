package com.syncra.gestion_proyectos.repository.document;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.document.DocumentEntity;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    /** Obtiene todos los documentos activos de un proyecto (sin eliminar) */
    List<DocumentEntity> findByProjectIdAndDeletedAtIsNull(Long projectId);

    /** Busca un documento activo por id */
    Optional<DocumentEntity> findByIdAndDeletedAtIsNull(Long id);
    /**lista los docuemntos por nombre */
    List<DocumentEntity> findByProjectIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(Long projectId, String title);
}