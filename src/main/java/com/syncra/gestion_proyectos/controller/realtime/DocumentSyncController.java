package com.syncra.gestion_proyectos.controller.realtime;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.syncra.gestion_proyectos.dto.realtime.DocumentSyncCatchUp;
import com.syncra.gestion_proyectos.dto.realtime.DocumentSyncMessage;
import com.syncra.gestion_proyectos.entity.document.DocumentEntity;
import com.syncra.gestion_proyectos.repository.document.DocumentRepository;
import com.syncra.gestion_proyectos.repository.project.ProjectMemberRepository;
import com.syncra.gestion_proyectos.service.realtime.DocumentCollaborationSessionRegistry;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Controller
@RequiredArgsConstructor
@Log4j2
public class DocumentSyncController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DocumentRepository documentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final DocumentCollaborationSessionRegistry sessionRegistry;

    private final Map<Long, DocumentSyncState> syncStates = new java.util.concurrent.ConcurrentHashMap<>();

    private static final String SYNC_TOPIC_PREFIX = "/topic/documents/";

    @MessageMapping("/documents/{documentId}/sync")
    public void sync(
            @DestinationVariable Long documentId,
            @Payload DocumentSyncMessage message,
            SimpMessageHeaderAccessor headers,
            Principal principal) {

        Long userId = obtenerUserId(headers);
        if (userId == null || message == null || (!hasText(message.getUpdate()) && !hasText(message.getAwareness()))) {
            log.debug("Update WebSocket descartado por entrada incompleta: documentId={}, userId={}, principalPresent={}, type={}, updatePresent={}, awarenessPresent={}",
                    documentId, userId, principal != null, message != null ? message.getType() : null,
                    message != null && hasText(message.getUpdate()), message != null && hasText(message.getAwareness()));
            return;
        }

        DocumentEntity document = documentRepository.findByIdAndDeletedAtIsNull(documentId).orElse(null);
        if (document == null || !projectMemberRepository.existsByIdProjectIdAndIdUserId(document.getProjectId(), userId)) {
            log.warn("Update WebSocket descartado por falta de pertenencia: documentId={}, userId={}", documentId, userId);
            return;
        }

        log.debug("Sync Yjs aceptado para documentId={}, userId={}, type={}, principalPresent={}",
                documentId, userId, message.getType(), principal != null);

        DocumentSyncState state = syncStates.computeIfAbsent(documentId, ignored -> new DocumentSyncState());
        if ("seed".equals(message.getType())) {
            synchronized (state) {
                if (state.seeded || !sessionRegistry.hasSessions(documentId)) {
                    return;
                }
                state.seeded = true;
                state.updates.add(copy(message));
            }
        } else if (hasText(message.getUpdate()) && !"awareness".equals(message.getType())) {
            synchronized (state) {
                state.updates.add(copy(message));
            }
        }

        DocumentSyncMessage outbound = new DocumentSyncMessage();
        outbound.setType(message.getType());
        outbound.setUpdate(message.getUpdate());
        outbound.setAwareness(message.getAwareness());
        outbound.setSenderSessionId(headers.getSessionId());

        log.debug("Broadcasting live Yjs sync to topic documentId={}, type={}, senderSessionId={}, updatePresent={}, awarenessPresent={}",
                documentId, message.getType(), headers.getSessionId(), hasText(message.getUpdate()), hasText(message.getAwareness()));

        messagingTemplate.convertAndSend(
                "/topic/documents/" + documentId + "/sync",
                outbound);
    }

    @EventListener
    public void onSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headers.getDestination();
        Long documentId = extractDocumentId(destination);
        String sessionId = headers.getSessionId();
        Long userId = obtenerUserId(headers);

        if (documentId == null || sessionId == null || userId == null
                || !destination.endsWith("/sync")) {
            return;
        }

        DocumentEntity document = documentRepository.findByIdAndDeletedAtIsNull(documentId).orElse(null);
        if (document == null || !projectMemberRepository.existsByIdProjectIdAndIdUserId(document.getProjectId(), userId)) {
            return;
        }

        sessionRegistry.register(documentId, sessionId);
        DocumentSyncState state = syncStates.computeIfAbsent(documentId, ignored -> new DocumentSyncState());
        List<DocumentSyncMessage> updates;
        boolean puedeSembrar;
        synchronized (state) {
            updates = state.updates.stream().map(this::copy).toList();
            puedeSembrar = updates.isEmpty() && !state.seeded;
        }

        sendCatchUp(sessionId, new DocumentSyncCatchUp(documentId, updates, puedeSembrar));
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        syncStates.forEach((documentId, state) -> {
            sessionRegistry.unregister(documentId, sessionId);
            if (!sessionRegistry.hasSessions(documentId)) {
                syncStates.remove(documentId, state);
            }
        });
    }

    private void sendCatchUp(String sessionId, DocumentSyncCatchUp catchUp) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        messagingTemplate.convertAndSendToUser(
                sessionId,
                "/queue/sync-catchup",
                catchUp,
                headers.getMessageHeaders());
    }

    private Long extractDocumentId(String destination) {
        if (destination == null || !destination.startsWith(SYNC_TOPIC_PREFIX)) {
            return null;
        }
        String[] parts = destination.split("/");
        if (parts.length != 5 || !"documents".equals(parts[2]) || !"sync".equals(parts[4])) {
            return null;
        }
        try {
            return Long.valueOf(parts[3]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private DocumentSyncMessage copy(DocumentSyncMessage source) {
        DocumentSyncMessage copy = new DocumentSyncMessage();
        copy.setType(source.getType());
        copy.setUpdate(source.getUpdate());
        copy.setAwareness(source.getAwareness());
        copy.setSenderSessionId(source.getSenderSessionId());
        return copy;
    }

    private static final class DocumentSyncState {
        private final List<DocumentSyncMessage> updates = new ArrayList<>();
        private boolean seeded;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Long obtenerUserId(SimpMessageHeaderAccessor headers) {
        Object userId = headers.getSessionAttributes().get("userId");
        return userId instanceof Number ? ((Number) userId).longValue() : null;
    }

}
