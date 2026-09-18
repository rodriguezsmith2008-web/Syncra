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
import com.syncra.gestion_proyectos.service.realtime.DocTemplateRealtimeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocTemplateService {

    private final DocTemplateRepository docTemplateRepository;

    private final DocTemplateSectionRepository docTemplateSectionRepository;

    private final DocTemplateRealtimeService docTemplateRealtimeService;

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
            .findRootPublishedTemplates();
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
        template.setDraftContent(template.getDefaultContent());
        template.setDraftTitle(template.getTitle());
        template.setDraftDescription(template.getDescription());
        template.setParentTemplateId(request.getParentTemplateId());
        template.setPublished(false);
        template.setPosition(docTemplateRepository.findAll().stream()
                .mapToLong(item -> item.getPosition() == null ? 0L : item.getPosition())
                .max().orElse(0L) + 1);
        DocTemplateEntity saved = docTemplateRepository.save(template);
        docTemplateRealtimeService.publishChanged(saved.getId(), "CREATED");
        return toResponse(saved, true);
    }

    public DocTemplateResponseDTO update(Long id, DocTemplateCreateDTO request) {
        DocTemplateEntity template = docTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe"));
        template.setDraftTitle(request.getTitle().trim());
        template.setDraftDescription(request.getDescription());
        template.setDraftContent(request.getDefaultContent() == null ? "<p></p>" : request.getDefaultContent());
        if (request.getParentTemplateId() != null) {
            template.setParentTemplateId(request.getParentTemplateId());
        }
        DocTemplateEntity saved = docTemplateRepository.save(template);
        docTemplateRealtimeService.publishChanged(saved.getId(), "UPDATED");
        return toDraftResponse(saved);
    }

    public DocTemplateResponseDTO publish(Long id) {
        DocTemplateEntity template = docTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe"));
        if (template.getDraftContent() != null) {
            template.setDefaultContent(template.getDraftContent());
        }
        if (template.getDraftTitle() != null) {
            template.setTitle(template.getDraftTitle());
        }
        if (template.getDraftDescription() != null) {
            template.setDescription(template.getDraftDescription());
        }
        template.setDraftContent(null);
        template.setDraftTitle(null);
        template.setDraftDescription(null);
        template.setPublished(true);
        DocTemplateEntity saved = docTemplateRepository.save(template);
        docTemplateRealtimeService.publishChanged(saved.getId(), "PUBLISHED");
        return toResponse(saved, true);
    }

    public void delete(Long id) {
        DocTemplateEntity template = docTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe"));
        docTemplateRepository.delete(template);
        docTemplateRealtimeService.publishChanged(id, "DELETED");
    }

    public DocTemplateResponseDTO getDraftById(Long id) {
        DocTemplateEntity template = docTemplateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("La plantilla no existe"));
        return toDraftResponse(template);
    }

    private DocTemplateResponseDTO toDraftResponse(DocTemplateEntity template) {
        DocTemplateResponseDTO response = toResponse(template, true);
        response.setTitle(template.getDraftTitle() != null ? template.getDraftTitle() : template.getTitle());
        response.setDescription(template.getDraftDescription() != null
            ? template.getDraftDescription() : template.getDescription());
        response.setDefaultContent(template.getDraftContent() != null
            ? template.getDraftContent() : template.getDefaultContent());
        return response;
    }

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

        List<DocTemplateSectionDTO> response = new ArrayList<>();
        List<DocTemplateSectionEntity> sections;
        try {
            sections = docTemplateSectionRepository.findByTemplateIdOrderByPositionAsc(templateId);
        } catch (DataAccessException exception) {
            return response;
        }

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