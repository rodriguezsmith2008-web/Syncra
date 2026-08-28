package com.syncra.gestion_proyectos.repository.chat;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.syncra.gestion_proyectos.entity.chat.PrivateConversationEntity;

public interface PrivateConversationRepository extends JpaRepository<PrivateConversationEntity, Long> {

    List<PrivateConversationEntity> findByProjectIdAndUserOneIdOrProjectIdAndUserTwoId(
            Long projectIdOne, Long userOneId, Long projectIdTwo, Long userTwoId);

    Optional<PrivateConversationEntity> findByProjectIdAndUserOneIdAndUserTwoId(
            Long projectId, Long userOneId, Long userTwoId);

    Optional<PrivateConversationEntity> findByIdAndProjectId(Long id, Long projectId);
}
