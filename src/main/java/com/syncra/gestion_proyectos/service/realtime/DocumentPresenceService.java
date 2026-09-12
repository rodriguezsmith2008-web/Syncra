package com.syncra.gestion_proyectos.service.realtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.syncra.gestion_proyectos.dto.realtime.DocumentPresenceEvent;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class DocumentPresenceService {

    private static final Pattern SYNC_DESTINATION =
            Pattern.compile("^/topic/documents/(\\d+)/sync$");

    private static final List<String> COLORS = List.of(
            "#2563eb", "#16a34a", "#dc2626", "#9333ea", "#ea580c", "#0891b2");

    private final SimpMessagingTemplate messagingTemplate;
    private final UsersRepository usersRepository;
    private final DocumentCollaborationSessionRegistry sessionRegistry;

    private final Map<Long, Map<String, Presence>> presenceByDocument = new ConcurrentHashMap<>();

    @EventListener
    public void onSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headers = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headers.getDestination();
        Long documentId = extractDocumentId(destination);
        Long userId = getUserId(headers);

        if (documentId == null || userId == null || headers.getSessionId() == null) {
            return;
        }

        UsersEntity user = usersRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        Presence presence = new Presence(
                documentId,
                userId,
                user.getFirstName() + " " + user.getLastName(),
                COLORS.get(Math.floorMod(userId.intValue(), COLORS.size())),
                user.getRole().name());

        Map<String, Presence> documentPresence = presenceByDocument.computeIfAbsent(
                documentId, ignored -> new ConcurrentHashMap<>());
        sessionRegistry.register(documentId, headers.getSessionId());
        Presence previous = documentPresence.putIfAbsent(headers.getSessionId(), presence);

        if (previous == null) {
            publish("JOINED", presence);
            log.info("Usuario conectado al documento: documentId={}, userId={}, sessionId={}",
                    documentId, userId, headers.getSessionId());
        }
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        List<Presence> removed = new ArrayList<>();

        presenceByDocument.forEach((documentId, documentPresence) -> {
            Presence presence = documentPresence.remove(sessionId);
            sessionRegistry.unregister(documentId, sessionId);
            if (presence != null) {
                removed.add(presence);
            }
            if (documentPresence.isEmpty()) {
                presenceByDocument.remove(documentId, documentPresence);
            }
        });

        removed.forEach(presence -> publish("LEFT", presence));
    }

    private void publish(String type, Presence presence) {
        messagingTemplate.convertAndSend(
                "/topic/documents/" + presence.documentId() + "/presence",
                new DocumentPresenceEvent(
                        type,
                        presence.documentId(),
                        presence.userId(),
                        presence.name(),
                        presence.color(),
                        presence.role()));
    }

    private Long extractDocumentId(String destination) {
        if (destination == null) {
            return null;
        }

        Matcher matcher = SYNC_DESTINATION.matcher(destination);
        return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
    }

    private Long getUserId(StompHeaderAccessor headers) {
        Object userId = headers.getSessionAttributes().get("userId");
        return userId instanceof Number ? ((Number) userId).longValue() : null;
    }

    private record Presence(Long documentId, Long userId, String name, String color, String role) {
    }
}
