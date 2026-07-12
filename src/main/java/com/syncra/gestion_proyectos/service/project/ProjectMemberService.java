package com.syncra.gestion_proyectos.service.project;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.project.ProjectMemberResponseDTO;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberId;
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
        return memberRepository.findByIdProjectId(projectId)
                .stream()
                .map(this::toResponse)
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

        return toResponse(memberRepository.save(entity));
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        if (!memberRepository.existsByIdProjectIdAndIdUserId(projectId, userId)) {
            throw new EntityNotFoundException("Miembro no encontrado en el proyecto");
        }
        memberRepository.deleteByIdProjectIdAndIdUserId(projectId, userId);
    }

    private ProjectMemberResponseDTO toResponse(ProjectMemberEntity e) {
        ProjectMemberResponseDTO r = new ProjectMemberResponseDTO();
        r.setProjectId(e.getId().getProjectId());
        r.setUserId(e.getId().getUserId());
        return r;
    }
}