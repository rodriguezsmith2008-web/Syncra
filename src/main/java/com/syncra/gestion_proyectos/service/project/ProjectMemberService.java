package com.syncra.gestion_proyectos.service.project;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.project.ProjectMemberResponseDTO;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberId;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectMemberService {

    private final ProjectMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final UsersRepository userRepository;

    public List<ProjectMemberResponseDTO> getMembers(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new EntityNotFoundException("Proyecto no encontrado");
        }

        List<ProjectMemberEntity> members = memberRepository.findByIdProjectId(projectId);

        
        List<Long> userIds = members.stream()
                .map(m -> m.getId().getUserId())
                .toList();

        Map<Long, UsersEntity> usersById = userRepository.findAllById(userIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(UsersEntity::getId, u -> u));

        return members.stream()
                .map(m -> toResponse(m, usersById.get(m.getId().getUserId())))
                .toList();
    }

    @Transactional
    public ProjectMemberResponseDTO addMember(Long projectId, Long userId) {
        if (!projectRepository.existsById(projectId)) {
            throw new EntityNotFoundException("Proyecto no encontrado");
        }
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Usuario no encontrado");
        }
        if (memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId)) {
            throw new IllegalStateException("El usuario ya es miembro del proyecto");
        }

        ProjectMemberEntity entity = new ProjectMemberEntity();
        entity.setId(new ProjectMemberId(projectId, userId));

        ProjectMemberEntity saved = memberRepository.save(entity);
        UsersEntity user = userRepository.findById(userId).orElse(null);

        return toResponse(saved, user);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        if (!memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId)) {
            throw new EntityNotFoundException("Miembro no encontrado en el proyecto");
        }
        memberRepository.deleteByIdProjectIdAndIdUserId(projectId, userId);
    }

    private ProjectMemberResponseDTO toResponse(ProjectMemberEntity e, UsersEntity user) {
        ProjectMemberResponseDTO r = new ProjectMemberResponseDTO();
        r.setProjectId(e.getId().getProjectId());
        r.setUserId(e.getId().getUserId());

        if (user != null) {
            r.setFirstName(user.getFirstName());
            r.setLastName(user.getLastName());
        }

        return r;
    }

    /**
     * Cambia a un usuario de un proyecto a otro
     *
     * @param currentProjectId proyecto del que se saca al usuario
     * @param newProjectId     proyecto al que se agrega el usuario
     * @param userId
     * @return miembro agregado al nuevo proyecto
     */
    @Transactional
    public ProjectMemberResponseDTO moveMember(Long currentProjectId, Long newProjectId, Long userId) {

        if (!memberRepository.existsByIdProjectIdAndIdUserId(currentProjectId, userId)) {
            throw new EntityNotFoundException("El usuario no es miembro del proyecto actual");
        }
        if (!projectRepository.existsById(newProjectId)) {
            throw new EntityNotFoundException("Proyecto destino no encontrado");
        }
        if (memberRepository.existsByIdProjectIdAndIdUserId(newProjectId, userId)) {
            throw new IllegalStateException("El usuario ya es miembro del proyecto destino");
        }

        memberRepository.deleteByIdProjectIdAndIdUserId(currentProjectId, userId);

        ProjectMemberEntity entity = new ProjectMemberEntity();
        entity.setId(new ProjectMemberId(newProjectId, userId));

        ProjectMemberEntity saved = memberRepository.save(entity);

        ProjectMemberResponseDTO dto = new ProjectMemberResponseDTO();
        dto.setProjectId(saved.getId().getProjectId());
        dto.setUserId(saved.getId().getUserId());

        return dto;
    }
}