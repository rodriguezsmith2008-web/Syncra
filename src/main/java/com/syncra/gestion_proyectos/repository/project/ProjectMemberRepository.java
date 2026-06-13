package com.syncra.gestion_proyectos.repository.project;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.syncra.gestion_proyectos.entity.project.ProjectMemberEntity;
import com.syncra.gestion_proyectos.entity.project.ProjectMemberId;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMemberEntity, ProjectMemberId> {

    List<ProjectMemberEntity> findByIdProjectId(Long projectId);

    List<ProjectMemberEntity> findByIdUserId(Long userId);

    boolean existsByIdProjectIdAndIdUserId(Long projectId, Long userId);

    void deleteByIdProjectIdAndIdUserId(Long projectId, Long userId);

}