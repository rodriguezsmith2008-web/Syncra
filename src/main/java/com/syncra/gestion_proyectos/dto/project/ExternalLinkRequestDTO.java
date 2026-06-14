package com.syncra.gestion_proyectos.dto.project;

import lombok.Data;

@Data
public class ExternalLinkRequestDTO {

    /**
     * Dto utilizado para recivir la información de un enlace externo asociado a un projecto 
     */
    private String title;
    private String url;

}
