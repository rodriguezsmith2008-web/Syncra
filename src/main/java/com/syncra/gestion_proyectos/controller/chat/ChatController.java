package com.syncra.gestion_proyectos.controller.chat;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.chat.ChatMessageRequestDTO;
import com.syncra.gestion_proyectos.dto.chat.ChatMessageResponseDTO;
import com.syncra.gestion_proyectos.dto.chat.ConversationResponseDTO;
import com.syncra.gestion_proyectos.service.chat.ChatService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/projects/{projectId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessageResponseDTO>> getProjectMessages(
            @PathVariable Long projectId, HttpServletRequest request) {
        return ResponseEntity.ok(chatService.getProjectMessages(projectId, getUserId(request)));
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponseDTO> sendProjectMessage(
            @PathVariable Long projectId,
            @Valid @RequestBody ChatMessageRequestDTO body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendProjectMessage(projectId, getUserId(request), body));
    }

            @PutMapping("/messages/{messageId}")
            public ResponseEntity<ChatMessageResponseDTO> editProjectMessage(
                @PathVariable Long projectId,
                @PathVariable Long messageId,
                @Valid @RequestBody ChatMessageRequestDTO body,
                HttpServletRequest request) {
            return ResponseEntity.ok(chatService.editProjectMessage(
                projectId, getUserId(request), messageId, body));
            }

            @DeleteMapping("/messages/{messageId}")
            public ResponseEntity<ChatMessageResponseDTO> deleteProjectMessage(
                @PathVariable Long projectId,
                @PathVariable Long messageId,
                HttpServletRequest request) {
            return ResponseEntity.ok(chatService.deleteProjectMessage(
                projectId, getUserId(request), messageId));
            }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponseDTO>> getConversations(
            @PathVariable Long projectId, HttpServletRequest request) {
        return ResponseEntity.ok(chatService.getConversations(projectId, getUserId(request)));
    }

    @PostMapping("/conversations/with/{otherUserId}")
    public ResponseEntity<ConversationResponseDTO> getOrCreateConversation(
            @PathVariable Long projectId,
            @PathVariable Long otherUserId,
            HttpServletRequest request) {
        return ResponseEntity.ok(chatService.getOrCreateConversation(
                projectId, getUserId(request), otherUserId));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageResponseDTO>> getPrivateMessages(
            @PathVariable Long projectId,
            @PathVariable Long conversationId,
            HttpServletRequest request) {
        return ResponseEntity.ok(chatService.getPrivateMessages(
                projectId, getUserId(request), conversationId));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ChatMessageResponseDTO> sendPrivateMessage(
            @PathVariable Long projectId,
            @PathVariable Long conversationId,
            @Valid @RequestBody ChatMessageRequestDTO body,
            HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.sendPrivateMessage(
                        projectId, getUserId(request), conversationId, body));
    }

                @PutMapping("/conversations/{conversationId}/messages/{messageId}")
                public ResponseEntity<ChatMessageResponseDTO> editPrivateMessage(
                    @PathVariable Long projectId,
                    @PathVariable Long conversationId,
                    @PathVariable Long messageId,
                    @Valid @RequestBody ChatMessageRequestDTO body,
                    HttpServletRequest request) {
                return ResponseEntity.ok(chatService.editPrivateMessage(
                    projectId, getUserId(request), conversationId, messageId, body));
                }

                @DeleteMapping("/conversations/{conversationId}/messages/{messageId}")
                public ResponseEntity<ChatMessageResponseDTO> deletePrivateMessage(
                    @PathVariable Long projectId,
                    @PathVariable Long conversationId,
                    @PathVariable Long messageId,
                    HttpServletRequest request) {
                return ResponseEntity.ok(chatService.deletePrivateMessage(
                    projectId, getUserId(request), conversationId, messageId));
                }

    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> markConversationAsRead(
            @PathVariable Long projectId,
            @PathVariable Long conversationId,
            HttpServletRequest request) {
        chatService.markConversationAsRead(projectId, getUserId(request), conversationId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }
}
