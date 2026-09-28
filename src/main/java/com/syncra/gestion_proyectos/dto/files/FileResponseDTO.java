package com.syncra.gestion_proyectos.dto.files;

import com.syncra.gestion_proyectos.enums.FilesTypeEnum;

import lombok.Data;

/** DTO de respuesta con la información de un archivo */
@Data
public class FileResponseDTO {

    private Long id;
    private Long projectId;
    private String name;
    private String url;
    private FilesTypeEnum type;
    private Long uploadedBy;
}