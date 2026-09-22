package com.syncra.gestion_proyectos.service.chat;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.syncra.gestion_proyectos.dto.chat.ChatRealtimeEventDTO;
import com.syncra.gestion_proyectos.dto.chat.ChatMessageRequestDTO;
import com.syncra.gestion_proyectos.dto.chat.ChatMessageResponseDTO;
import com.syncra.gestion_proyectos.dto.chat.ConversationResponseDTO;
import com.syncra.gestion_proyectos.entity.chat.PrivateConversationEntity;
import com.syncra.gestion_proyectos.entity.chat.PrivateMessageEntity;
import com.syncra.gestion_proyectos.entity.chat.ProjectChatMessageEntity;
import com.syncra.gestion_proyectos.enums.ChatMessageTypeEnum;
import com.syncra.gestion_proyectos.repository.chat.PrivateConversationRepository;
import com.syncra.gestion_proyectos.repository.chat.PrivateMessageRepository;
import com.syncra.gestion_proyectos.repository.chat.ProjectChatMessageRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.service.notification.NotificationService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final String EDITED_MARKER = "__SYNCra_EDITED__";
    private static final String DELETED_MARKER = "__SYNCra_DELETED__";

    private final ProjectChatMessageRepository projectChatMessageRepository;
    private final PrivateConversationRepository privateConversationRepository;
    private final PrivateMessageRepository privateMessageRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UsersRepository usersRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public List<ChatMessageResponseDTO> getProjectMessages(Long projectId, Long userId) {
        validateMember(projectId, userId);
        List<ProjectChatMessageEntity> messages = projectChatMessageRepository
            .findByProjectIdOrderByCreatedAtAsc(projectId);
        Map<Long, com.syncra.gestion_proyectos.entity.user.UsersEntity> usersById = usersRepository.findAllById(
            messages.stream().map(ProjectChatMessageEntity::getUserId).distinct().toList())
            .stream().collect(Collectors.toMap(
                com.syncra.gestion_proyectos.entity.user.UsersEntity::getId, Function.identity()));
        return messages
                .stream()
            .map(message -> toResponse(message, usersById))
                .toList();
    }

    @Transactional
    public ChatMessageResponseDTO sendProjectMessage(
            Long projectId, Long userId, ChatMessageRequestDTO request) {
        return persistProjectMessage(
                projectId, userId, request.getContent().trim(), ChatMessageTypeEnum.TEXT);
    }

    @Transactional
    public ChatMessageResponseDTO startProjectCall(Long projectId, Long userId) {
        String roomName = "syncra-" + projectId + "-" + UUID.randomUUID();
        return persistProjectMessage(projectId, userId, roomName, ChatMessageTypeEnum.CALL_INVITE);
    }

    private ChatMessageResponseDTO persistProjectMessage(
            Long projectId, Long userId, String content, ChatMessageTypeEnum type) {

        validateMember(projectId, userId);

        ProjectChatMessageEntity entity = new ProjectChatMessageEntity();
        entity.setProjectId(projectId);
        entity.setUserId(userId);
        entity.setContent(content);
        entity.setType(type);

        ProjectChatMessageEntity saved = projectChatMessageRepository.save(entity);
        publishChatEvent(projectId, null, "PROJECT_MESSAGE");
        notifyProjectMembers(projectId, userId, "CHAT_PROJECT",
                type == ChatMessageTypeEnum.CALL_INVITE
                        ? "Se inició una videollamada en el chat del proyecto"
                        : "Nuevo mensaje en el chat del proyecto");
        return toResponse(saved);
    }

    @Transactional
    public ChatMessageResponseDTO editProjectMessage(
            Long projectId, Long userId, Long messageId, ChatMessageRequestDTO request) {
        validateMember(projectId, userId);
        ProjectChatMessageEntity message = projectChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Mensaje no encontrado"));
        if (!projectId.equals(message.getProjectId()) || !userId.equals(message.getUserId())) {
            throw new EntityNotFoundException("Mensaje no encontrado");
        }
        message.setContent(EDITED_MARKER + "\n" + request.getContent().trim());
        ChatMessageResponseDTO response = toResponse(projectChatMessageRepository.save(message));
        publishChatEvent(projectId, null, "PROJECT_MESSAGE");
        return response;
    }

    @Transactional
    public ChatMessageResponseDTO deleteProjectMessage(Long projectId, Long userId, Long messageId) {
        validateMember(projectId, userId);
        ProjectChatMessageEntity message = projectChatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Mensaje no encontrado"));
        if (!projectId.equals(message.getProjectId()) || !userId.equals(message.getUserId())) {
            throw new EntityNotFoundException("Mensaje no encontrado");
        }
        message.setContent(DELETED_MARKER);
        ChatMessageResponseDTO response = toResponse(projectChatMessageRepository.save(message));
        publishChatEvent(projectId, null, "PROJECT_MESSAGE");
        return response;
    }

    public List<ConversationResponseDTO> getConversations(Long projectId, Long userId) {
        validateMember(projectId, userId);
        return privateConversationRepository
                .findByProjectIdAndUserOneIdOrProjectIdAndUserTwoId(projectId, userId, projectId, userId)
                .stream()
                .sorted(Comparator.comparing(PrivateConversationEntity::getCreatedAt))
                .map(conversation -> toConversationResponse(conversation, userId))
                .toList();
    }

    @Transactional
    public ConversationResponseDTO getOrCreateConversation(
            Long projectId, Long userId, Long otherUserId) {

        validateMember(projectId, userId);
        validateMember(projectId, otherUserId);

        if (userId.equals(otherUserId)) {
            throw new IllegalArgumentException("No puedes crear una conversación contigo mismo");
        }

        Long userOneId = Math.min(userId, otherUserId);
        Long userTwoId = Math.max(userId, otherUserId);

        PrivateConversationEntity conversation = privateConversationRepository
                .findByProjectIdAndUserOneIdAndUserTwoId(projectId, userOneId, userTwoId)
                .orElseGet(() -> {
                    PrivateConversationEntity newConversation = new PrivateConversationEntity();
                    newConversation.setProjectId(projectId);
                    newConversation.setUserOneId(userOneId);
                    newConversation.setUserTwoId(userTwoId);
                    return privateConversationRepository.save(newConversation);
                });

        return toConversationResponse(conversation, userId);
    }

    public List<ChatMessageResponseDTO> getPrivateMessages(
            Long projectId, Long userId, Long conversationId) {

        validateMember(projectId, userId);
        PrivateConversationEntity conversation = getConversation(projectId, conversationId);
        validateConversationMember(conversation, userId);

        List<PrivateMessageEntity> messages = privateMessageRepository
            .findByConversationIdOrderByCreatedAtAsc(conversationId);
        Map<Long, com.syncra.gestion_proyectos.entity.user.UsersEntity> usersById = usersRepository.findAllById(
            messages.stream().map(PrivateMessageEntity::getSenderId).distinct().toList())
            .stream().collect(Collectors.toMap(
                com.syncra.gestion_proyectos.entity.user.UsersEntity::getId, Function.identity()));
        return messages
                .stream()
            .map(message -> toResponse(message, usersById))
                .toList();
    }

    @Transactional
    public ChatMessageResponseDTO sendPrivateMessage(
            Long projectId, Long userId, Long conversationId, ChatMessageRequestDTO request) {
        return persistPrivateMessage(
                projectId, userId, conversationId, request.getContent().trim(), ChatMessageTypeEnum.TEXT);
    }

    @Transactional
    public ChatMessageResponseDTO startPrivateCall(Long projectId, Long userId, Long conversationId) {
        String roomName = "syncra-conv-" + conversationId + "-" + UUID.randomUUID();
        return persistPrivateMessage(projectId, userId, conversationId, roomName, ChatMessageTypeEnum.CALL_INVITE);
    }

    private ChatMessageResponseDTO persistPrivateMessage(
            Long projectId, Long userId, Long conversationId, String content, ChatMessageTypeEnum type) {

        validateMember(projectId, userId);
        PrivateConversationEntity conversation = getConversation(projectId, conversationId);
        validateConversationMember(conversation, userId);

        PrivateMessageEntity entity = new PrivateMessageEntity();
        entity.setConversationId(conversationId);
        entity.setSenderId(userId);
        entity.setContent(content);
        entity.setType(type);
        entity.setRead(false);

        PrivateMessageEntity saved = privateMessageRepository.save(entity);
        Long recipientId = userId.equals(conversation.getUserOneId())
            ? conversation.getUserTwoId()
            : conversation.getUserOneId();
        notificationService.crear(recipientId, userId, projectId, null, null, null,
            "CHAT_PRIVATE",
            type == ChatMessageTypeEnum.CALL_INVITE ? "Te está llamando" : "Nuevo mensaje privado");
        publishChatEvent(projectId, conversationId, "PRIVATE_MESSAGE");
        return toResponse(saved);
    }

    @Transactional
    public ChatMessageResponseDTO editPrivateMessage(
            Long projectId, Long userId, Long conversationId, Long messageId,
            ChatMessageRequestDTO request) {
        validateMember(projectId, userId);
        PrivateConversationEntity conversation = getConversation(projectId, conversationId);
        validateConversationMember(conversation, userId);
        PrivateMessageEntity message = privateMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Mensaje no encontrado"));
        if (!conversationId.equals(message.getConversationId()) || !userId.equals(message.getSenderId())) {
            throw new EntityNotFoundException("Mensaje no encontrado");
        }
        message.setContent(EDITED_MARKER + "\n" + request.getContent().trim());
        ChatMessageResponseDTO response = toResponse(privateMessageRepository.save(message));
        publishChatEvent(projectId, conversationId, "PRIVATE_MESSAGE");
        return response;
    }

    @Transactional
    public ChatMessageResponseDTO deletePrivateMessage(
            Long projectId, Long userId, Long conversationId, Long messageId) {
        validateMember(projectId, userId);
        PrivateConversationEntity conversation = getConversation(projectId, conversationId);
        validateConversationMember(conversation, userId);
        PrivateMessageEntity message = privateMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Mensaje no encontrado"));
        if (!conversationId.equals(message.getConversationId()) || !userId.equals(message.getSenderId())) {
            throw new EntityNotFoundException("Mensaje no encontrado");
        }
        message.setContent(DELETED_MARKER);
        ChatMessageResponseDTO response = toResponse(privateMessageRepository.save(message));
        publishChatEvent(projectId, conversationId, "PRIVATE_MESSAGE");
        return response;
    }

    @Transactional
    public void markConversationAsRead(Long projectId, Long userId, Long conversationId) {

        validateMember(projectId, userId);
        PrivateConversationEntity conversation = getConversation(projectId, conversationId);
        validateConversationMember(conversation, userId);

        List<PrivateMessageEntity> messages = privateMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId);

        messages.stream()
                .filter(message -> !userId.equals(message.getSenderId()))
                .forEach(message -> message.setRead(true));

        privateMessageRepository.saveAll(messages);
    }

    private PrivateConversationEntity getConversation(Long projectId, Long conversationId) {
        return privateConversationRepository.findByIdAndProjectId(conversationId, projectId)
                .orElseThrow(() -> new EntityNotFoundException("Conversación no encontrada"));
    }

    private void validateMember(Long projectId, Long userId) {
        if (!projectMemberRepository.existsByIdProjectIdAndIdUserId(projectId, userId)) {
            throw new EntityNotFoundException("El usuario no pertenece al proyecto");
        }
    }

    private void publishChatEvent(Long projectId, Long conversationId, String type) {
        Runnable publish = () -> messagingTemplate.convertAndSend(
                "/topic/projects/" + projectId + "/chat",
                new ChatRealtimeEventDTO(projectId, conversationId, type));
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish.run();
            }
        });
    }

    private void validateConversationMember(PrivateConversationEntity conversation, Long userId) {
        if (!userId.equals(conversation.getUserOneId())
                && !userId.equals(conversation.getUserTwoId())) {
            throw new EntityNotFoundException("El usuario no pertenece a la conversación");
        }
    }

    private void notifyProjectMembers(Long projectId, Long actorId, String type, String message) {
        projectMemberRepository.findByIdProjectId(projectId).stream()
                .map(member -> member.getId().getUserId())
                .filter(memberId -> !memberId.equals(actorId))
                .forEach(memberId -> notificationService.crear(
                        memberId, actorId, projectId, null, null, null, type, message));
    }

    private ChatMessageResponseDTO toResponse(ProjectChatMessageEntity entity) {
        return toResponse(entity, usersRepository.findById(entity.getUserId()).orElse(null));
        }

        private ChatMessageResponseDTO toResponse(
            ProjectChatMessageEntity entity,
            Map<Long, com.syncra.gestion_proyectos.entity.user.UsersEntity> usersById) {
            return toResponse(entity, usersById.get(entity.getUserId()));
            }

            private ChatMessageResponseDTO toResponse(
                ProjectChatMessageEntity entity,
                com.syncra.gestion_proyectos.entity.user.UsersEntity user) {
        ChatMessageResponseDTO response = new ChatMessageResponseDTO();
        response.setId(entity.getId());
        response.setProjectId(entity.getProjectId());
        response.setUserId(entity.getUserId());
        setSenderData(response, user);
        response.setContent(entity.getContent());
        response.setType(entity.getType().name());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    private ChatMessageResponseDTO toResponse(PrivateMessageEntity entity) {
        return toResponse(entity, usersRepository.findById(entity.getSenderId()).orElse(null));
        }

        private ChatMessageResponseDTO toResponse(
            PrivateMessageEntity entity,
            Map<Long, com.syncra.gestion_proyectos.entity.user.UsersEntity> usersById) {
            return toResponse(entity, usersById.get(entity.getSenderId()));
            }

            private ChatMessageResponseDTO toResponse(
                PrivateMessageEntity entity,
                com.syncra.gestion_proyectos.entity.user.UsersEntity user) {
        ChatMessageResponseDTO response = new ChatMessageResponseDTO();
        response.setId(entity.getId());
        response.setConversationId(entity.getConversationId());
        response.setSenderId(entity.getSenderId());
        setSenderData(response, user);
        response.setContent(entity.getContent());
        response.setIsRead(entity.getRead());
        response.setType(entity.getType().name());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    private void setSenderData(ChatMessageResponseDTO response,
            com.syncra.gestion_proyectos.entity.user.UsersEntity user) {
        if (user != null) {
            response.setSenderFullName(user.getFirstName() + " " + user.getLastName());
            response.setSenderAvatarUrl(user.getAvatarUrl());
        }
    }

    private ConversationResponseDTO toConversationResponse(PrivateConversationEntity entity, Long userId) {
        ConversationResponseDTO response = new ConversationResponseDTO();
        response.setId(entity.getId());
        response.setProjectId(entity.getProjectId());
        response.setUserOneId(entity.getUserOneId());
        response.setUserTwoId(entity.getUserTwoId());
        response.setUnreadCount(privateMessageRepository
            .countByConversationIdAndSenderIdNotAndReadFalse(entity.getId(), userId));
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
    public long countUnreadPrivateMessages(
        Long projectId,
        Long userId,
        Long conversationId) {

    validateMember(projectId, userId);

    PrivateConversationEntity conversation =
            getConversation(projectId, conversationId);

    validateConversationMember(conversation, userId);

    return privateMessageRepository
            .countByConversationIdAndSenderIdNotAndReadFalse(
                    conversationId,
                    userId
            );
}
}
