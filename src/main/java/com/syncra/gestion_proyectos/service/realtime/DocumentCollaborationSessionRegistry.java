package com.syncra.gestion_proyectos.service.realtime;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class DocumentCollaborationSessionRegistry {

    private final Map<Long, Set<String>> sessionsByDocument = new ConcurrentHashMap<>();

    public void register(Long documentId, String sessionId) {
        sessionsByDocument.computeIfAbsent(documentId, ignored -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
    }

    public boolean unregister(Long documentId, String sessionId) {
        Set<String> sessions = sessionsByDocument.get(documentId);
        if (sessions == null || !sessions.remove(sessionId)) {
            return false;
        }

        if (sessions.isEmpty()) {
            sessionsByDocument.remove(documentId, sessions);
            return true;
        }

        return false;
    }

    public boolean hasSessions(Long documentId) {
        Set<String> sessions = sessionsByDocument.get(documentId);
        return sessions != null && !sessions.isEmpty();
    }
}
