package com.syncra.gestion_proyectos.controller.notification;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.notifications.NotificationResponseDTO;
import com.syncra.gestion_proyectos.service.notification.NotificationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    /**
     * Obtiene las notificaciones del usuario autenticado.
     *
     * @param unreadOnly
     * @param request usado para obtener el id del usuario autenticado
     * @return lista de notificaciones
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> getByUser(
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");

        if (unreadOnly) {
            return ResponseEntity.ok(service.getUnreadByUser(userId));
        }

        return ResponseEntity.ok(service.getByUser(userId));
    }

    /**
     * Marca una notificacion del usuario autenticado como leida
     *
     * @param id
     * @param request usado para obtener el id del usuario autenticado
     * @return notificacion actualizada
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponseDTO> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(service.markAsRead(id, userId));
    }
}