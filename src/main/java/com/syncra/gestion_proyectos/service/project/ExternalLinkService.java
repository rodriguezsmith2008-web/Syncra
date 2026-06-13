package com.syncra.gestion_proyectos.service.project;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.project.ExternalLinkRequestDTO;
import com.syncra.gestion_proyectos.dto.project.ExternalLinkResponseDTO;
import com.syncra.gestion_proyectos.entity.project.ExternalLinkEntity;
import com.syncra.gestion_proyectos.repository.project.ExternalLinkRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExternalLinkService {

    //se utiliza para acceder a la información que hay en el repositorio
    private final ExternalLinkRepository linkRepository;


    /**
     * Obtiene todos los enlaces asociados a un proyecto
     * @param projectId identificador del proyecto 
     * @return lista de enlaces externos al proyecto
     */
    public List<ExternalLinkResponseDTO> getByProject(Long projectId) {
        return linkRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Obtiene un enlace específico perteneciente a un proyecto
     * @param projectId 
     * @param linkId id del enlace
     * @return información del enlace solicitado
     */
    public ExternalLinkResponseDTO getById(Long projectId, Long linkId) {
        ExternalLinkEntity entity = findOrThrow(linkId);
        validateBelongsToProject(entity, projectId);
        return toResponse(entity);
    }

    /**
     * Crea un nuevo enlace externo para un proyecto
     * @param projectId
     * @param dto datos del enlace a registrar
     * @param addedBy usuario que registra el enlace
     * @return información del enlace creado
     */
    @Transactional
    public ExternalLinkResponseDTO create(Long projectId, ExternalLinkRequestDTO dto, Long addedBy) {
        ExternalLinkEntity entity = new ExternalLinkEntity();
        entity.setProjectId(projectId);
        entity.setTitle(dto.getTitle());
        entity.setUrl(dto.getUrl());
        entity.setAddedBy(addedBy);
        return toResponse(linkRepository.save(entity));
    }

    /**
     * Actualiza la información de un enlace existente
     * @param projectId
     * @param linkId
     * @param dto
     * @return información actualizada del enlace
     */
    @Transactional
    public ExternalLinkResponseDTO update(Long projectId, Long linkId, ExternalLinkRequestDTO dto) {
        ExternalLinkEntity entity = findOrThrow(linkId);
        validateBelongsToProject(entity, projectId);
        if (dto.getTitle() != null)
            entity.setTitle(dto.getTitle());
        if (dto.getUrl() != null)
            entity.setUrl(dto.getUrl());
        return toResponse(linkRepository.save(entity));
    }

    /**
     * Elimina un enlace asociado a un proyecto
     * @param projectId
     * @param linkId
     */
    @Transactional
    public void delete(Long projectId, Long linkId) {
        ExternalLinkEntity entity = findOrThrow(linkId);
        validateBelongsToProject(entity, projectId);
        linkRepository.delete(entity);
    }

    /**
     * Busca un enlace por su identificador
     * @param id id del enlace
     * @return entidad encontrada
     */
    private ExternalLinkEntity findOrThrow(Long id) {
        return linkRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ExternalLink not found: " + id));
    }

    /**
     * Verifica que el enlace pertenezca al proyecto
     * @param entity entidad del enlace
     * @param projectId id del proyecto
     */
    private void validateBelongsToProject(ExternalLinkEntity entity, Long projectId) {
        if (!entity.getProjectId().equals(projectId)) {
            throw new EntityNotFoundException("El enlace no pertenece al proyecto: " + projectId);
        }
    }

    /**
     * Convierte una entidad en un dto de respuesta
     * @param e entidad a convertir
     * @return dto con la información del enlace
     */
    private ExternalLinkResponseDTO toResponse(ExternalLinkEntity e) {
        ExternalLinkResponseDTO r = new ExternalLinkResponseDTO();
        r.setId(e.getId());
        r.setProjectId(e.getProjectId());
        r.setTitle(e.getTitle());
        r.setUrl(e.getUrl());
        r.setAddedBy(e.getAddedBy());
        r.setCreatedAt(e.getCreatedAt());
        return r;
    }
}
