package com.syncra.gestion_proyectos.dto.ai;

import java.time.LocalDateTime;
import com.syncra.gestion_proyectos.enums.AiRoleEnum;
import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
public class AiMessageResponseDTO {
    private Long id;
    private Long conversationId;
    private AiRoleEnum role;
    private String content;
    private LocalDateTime createdAt;
    @JsonAlias("suggested_card")
    private AiSuggestedCardDTO suggestedCard;
    private List<String> executedActions;
}