package com.syncra.gestion_proyectos.repository.document;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.document.FilesEntity;

public interface FilesRepository extends JpaRepository<FilesEntity,Long> {
    
}
