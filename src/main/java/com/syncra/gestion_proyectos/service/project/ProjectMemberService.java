package com.syncra.gestion_proyectos.service.project;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.project.ProjectMemberResponseDTO;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberId;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    //Repositorio utilizado para acceder y gestionar la información de los miembros de los proyectos
    private final ProjectMemberRepository memberRepository;

    /**
     * Obtiene todos los miembros asociados a un proyecto
     * @param projectId id del proyecto
     * @return lista de miembros asociados al proyecto
     */
    public List<ProjectMemberResponseDTO> getMembers(Long projectId) {
        return memberRepository.findByIdProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Agrega un usuario como miembro de un proyecto
     * @param projectId 
     * @param userId id del usuario a agregar
     * @return información del miembro agregado
     */
    @Transactional
    public ProjectMemberResponseDTO addMember(Long projectId, Long userId) {
        if (memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId)) {
            throw new IllegalStateException("El usuario ya es miembro del proyecto");
        }

        ProjectMemberEntity entity = new ProjectMemberEntity();
        entity.setId(new ProjectMemberId(projectId, userId));

        return toResponse(memberRepository.save(entity));
    }

    /**
     * Elimina un miembro de un proyecto
     * @param projectId 
     * @param userId
     */
    @Transactional
    public void removeMember(Long projectId, Long userId) {
        if (!memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId)) {
            throw new EntityNotFoundException("Miembro no encontrado en el proyecto");
        }
        memberRepository.deleteByIdProjectIdAndIdUserId(projectId, userId);
    }

    /**
     * Convierte una entidad projectmemberentity en un dto de respuesta
     * @param e entidad que representa la relación entre proyecto y usuario
     * @return dto con la información del miembro del proyecto
     */
    private ProjectMemberResponseDTO toResponse(ProjectMemberEntity e) {
        ProjectMemberResponseDTO r = new ProjectMemberResponseDTO();
        r.setProjectId(e.getId().getProjectId());
        r.setUserId(e.getId().getUserId());
        return r;
    }
}
