package com.syncra.gestion_proyectos.repository.project;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.project.ProjectMemberEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberId;

public interface ProjectMemberRepository extends JpaRepository<ProjectMemberEntity, ProjectMemberId> {

    //obtiene todos los miembros asociados a un proyecto
    List<ProjectMemberEntity> findByIdProjectId(Long projectId);

    //obtiene todos los proyectos en los que participa un usuario
    List<ProjectMemberEntity> findByIdUserId(Long userId);

    //verifica si un usuario pertenece a un proyecto
    boolean existsByIdProjectIdAndIdUserId(Long projectId, Long userId);

    //elimina la asociación que hay entre un usuario y un proyecto
    void deleteByIdProjectIdAndIdUserId(Long projectId, Long userId);

    boolean existsByIdUserId(Long userId);


}