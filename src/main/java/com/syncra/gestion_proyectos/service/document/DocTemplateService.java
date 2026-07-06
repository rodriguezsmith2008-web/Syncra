package com.syncra.gestion_proyectos.service.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.document.DocTemplateResponseDTO;
import com.syncra.gestion_proyectos.entity.document.DocTemplateEntity;
import com.syncra.gestion_proyectos.repository.document.DocTemplateRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service de solo lectura para el catálogo de plantillas de documentos.
 * Las plantillas son fijas y se cargan mediante DocTemplateSeeder,
 * por lo que este servicio no expone creación, edición ni eliminación.
 */
@Service
@RequiredArgsConstructor
public class DocTemplateService {

    private final DocTemplateRepository docTemplateRepository;

    /**
     * Obtiene el catálogo completo de plantillas ordenado por posición.
     * No incluye el contenido base de cada plantilla (defaultContent),
     * solo la información necesaria para listarlas.
     *
     * @return lista de plantillas disponibles
     */
    public List<DocTemplateResponseDTO> listAll() {

        List<DocTemplateEntity> templates = docTemplateRepository.findAllByOrderByPositionAsc();
        List<DocTemplateResponseDTO> response = new ArrayList<>();

        for (DocTemplateEntity template : templates) {
            response.add(toResponse(template, false));
        }

        return response;
    }

    /**
     * Obtiene una plantilla puntual, incluyendo su contenido base,
     * útil para previsualizarla antes de crear un documento con ella.
     *
     * @param id
     * @return plantilla encontrada, null si no existe
     */
    public DocTemplateResponseDTO getById(Long id) {

        Optional<DocTemplateEntity> templateFound = docTemplateRepository.findById(id);

        if (templateFound.isEmpty()) {
            return null;
        }

        return toResponse(templateFound.get(), true);
    }

    /**
     * Convierte una entidad en un dto de respuesta
     *
     * @param template entidad a convertir
     * @param includeContent si se debe incluir el contenido base de la plantilla
     * @return dto con la información de la plantilla
     */
    private DocTemplateResponseDTO toResponse(DocTemplateEntity template, boolean includeContent) {

        DocTemplateResponseDTO response = new DocTemplateResponseDTO();

        response.setId(template.getId());
        response.setCode(template.getCode());
        response.setTitle(template.getTitle());
        response.setDescription(template.getDescription());
        response.setPosition(template.getPosition());

        if (includeContent) {
            response.setDefaultContent(template.getDefaultContent());
        }

        return response;
    }
}