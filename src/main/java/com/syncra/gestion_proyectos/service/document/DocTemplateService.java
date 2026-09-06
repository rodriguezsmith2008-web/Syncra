package com.syncra.gestion_proyectos.service.document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;

import com.syncra.gestion_proyectos.dto.document.DocTemplateResponseDTO;
import com.syncra.gestion_proyectos.dto.document.DocTemplateSectionDTO;
import com.syncra.gestion_proyectos.dto.document.DocTemplateCreateDTO;
import com.syncra.gestion_proyectos.entity.document.DocTemplateEntity;
import com.syncra.gestion_proyectos.entity.document.DocTemplateSectionEntity;
import com.syncra.gestion_proyectos.repository.document.DocTemplateRepository;
import com.syncra.gestion_proyectos.repository.document.DocTemplateSectionRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class DocTemplateService {

    private final DocTemplateRepository docTemplateRepository;

    private final DocTemplateSectionRepository docTemplateSectionRepository;

    /**
     * Obtiene el catálogo completo de plantillas ordenado por posición.
     * No incluye el contenido base de cada plantilla (defaultContent),
     * solo la información necesaria para listarlas. Sí incluye las
     * secciones (con su propio defaultContent), porque el frontend las
     * necesita para poder ofrecer la reutilización de información antes
     * de que el usuario entre a ver la plantilla completa.
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

    public List<DocTemplateResponseDTO> listPublished() {

        List<DocTemplateEntity> templates = docTemplateRepository
            .findAllByPublishedTrueAndParentTemplateIdIsNullOrderByPositionAsc();
        List<DocTemplateResponseDTO> response = new ArrayList<>();

        for (DocTemplateEntity template : templates) {
            String description = template.getDescription();
            boolean legacyChild = description != null
                    && description.trim().toLowerCase().startsWith("plantilla hija de");

            if (!legacyChild) {
                response.add(toResponse(template, false));
            }
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

    public DocTemplateResponseDTO create(DocTemplateCreateDTO request) {
        DocTemplateEntity template = new DocTemplateEntity();
        template.setCode("CUSTOM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        template.setTitle(request.getTitle().trim());
        template.setDescription(request.getDescription());
        template.setDefaultContent(request.getDefaultContent() == null ? "<p></p>" : request.getDefaultContent());
        template.setParentTemplateId(request.getParentTemplateId());
        template.setPublished(false);
        template.setPosition(docTemplateRepository.findAll().stream()
                .mapToLong(item -> item.getPosition() == null ? 0L : item.getPosition())
                .max().orElse(0L) + 1);
        return toResponse(docTemplateRepository.save(template), true);
    }

    public DocTemplateResponseDTO update(Long id, DocTemplateCreateDTO request) {
        DocTemplateEntity template = docTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe"));
        template.setTitle(request.getTitle().trim());
        template.setDescription(request.getDescription());
        template.setDefaultContent(request.getDefaultContent() == null ? "<p></p>" : request.getDefaultContent());
        template.setPublished(false);
        return toResponse(docTemplateRepository.save(template), true);
    }

    public DocTemplateResponseDTO publish(Long id) {
        DocTemplateEntity template = docTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe"));
        template.setPublished(true);
        return toResponse(docTemplateRepository.save(template), true);
    }

    public void delete(Long id) {
        DocTemplateEntity template = docTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe"));
        docTemplateRepository.delete(template);
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
        response.setPublished(template.isPublished());
        response.setParentTemplateId(template.getParentTemplateId());

        if (includeContent) {
            response.setDefaultContent(template.getDefaultContent());
        }

        response.setSections(getSections(template.getId()));

        return response;
    }

    private List<DocTemplateSectionDTO> getSections(Long templateId) {

        List<DocTemplateSectionEntity> sections;
        try {
            sections = docTemplateSectionRepository.findByTemplateIdOrderByPositionAsc(templateId);
        } catch (DataAccessException exception) {
            return new ArrayList<>();
        }

        List<DocTemplateSectionDTO> response = new ArrayList<>();

        for (DocTemplateSectionEntity section : sections) {

            DocTemplateSectionDTO dto = new DocTemplateSectionDTO();

            dto.setId(section.getId());
            dto.setTemplateId(section.getTemplateId());
            dto.setSectionKey(section.getSectionKey());
            dto.setTitle(section.getTitle());
            dto.setDefaultContent(section.getDefaultContent());
            dto.setPosition(section.getPosition());

            response.add(dto);
        }

        return response;
    }
}