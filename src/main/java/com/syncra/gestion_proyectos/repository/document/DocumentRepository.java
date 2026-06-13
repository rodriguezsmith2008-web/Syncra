package com.syncra.gestion_proyectos.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.document.DocumentEntity;

public interface DocumentRepository extends JpaRepository<DocumentEntity,Long>{

     
}