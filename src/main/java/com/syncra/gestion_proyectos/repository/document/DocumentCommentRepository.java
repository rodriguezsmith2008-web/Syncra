package com.syncra.gestion_proyectos.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.document.DocumentCommentEntity;

public interface DocumentCommentRepository extends JpaRepository<DocumentCommentEntity,Long>{

    
}
