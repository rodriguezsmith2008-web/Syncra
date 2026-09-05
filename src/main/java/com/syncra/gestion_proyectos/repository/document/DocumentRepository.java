package com.syncra.gestion_proyectos.repository.document;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.document.DocumentEntity;
import com.syncra.gestion_proyectos.enums.DocumentTypeEnum;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {

    /** Obtiene un documento activo por id */
    Optional<DocumentEntity> findByIdAndDeletedAtIsNull(Long id);

    /** Busca documentos por nombre */
    List<DocumentEntity> findByProjectIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(Long projectId, String title);

    /** Documentos principales (no subdocumentos) */
    List<DocumentEntity> findByProjectIdAndDocumentTypeAndParentDocumentIdIsNullAndDeletedAtIsNull(Long projectId,
            DocumentTypeEnum documentType);

    /** Subdocumentos */
    List<DocumentEntity> findByParentDocumentIdAndDeletedAtIsNullOrderBySortOrderAsc(Long parentDocumentId);

    /** Actas */
    List<DocumentEntity> findByProjectIdAndDocumentTypeAndDeletedAtIsNull(Long projectId,
            DocumentTypeEnum documentType);

    List<DocumentEntity> findByProjectIdAndDocumentTypeAndTitleContainingIgnoreCaseAndDeletedAtIsNull(Long projectId,
            DocumentTypeEnum documentType, String title);

    List<DocumentEntity> findByParentDocumentIdAndDocumentTypeAndDeletedAtIsNullOrderBySortOrderAsc(
            Long parentDocumentId,
            DocumentTypeEnum documentType);
            
    List<DocumentEntity> findByProjectIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long projectId);

}