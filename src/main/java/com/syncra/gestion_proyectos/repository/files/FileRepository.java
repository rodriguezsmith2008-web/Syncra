package com.syncra.gestion_proyectos.repository.files;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.files.FilesEntity;

public interface FileRepository extends JpaRepository<FilesEntity, Long> {

    /**
     * Obtiene todos los archivos de un proyecto
     *
     * @param projectId
     * @return lista de archivos
     */
    List<FilesEntity> findByProjectId(Long projectId);
}