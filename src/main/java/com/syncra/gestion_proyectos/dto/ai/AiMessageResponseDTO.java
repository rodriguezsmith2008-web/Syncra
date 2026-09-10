package com.syncra.gestion_proyectos.dto.ai;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.syncra.gestion_proyectos.enums.AiRoleEnum;

import lombok.Data;

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
    private List<AiToolCallDTO> toolCalls;
    private String model;
    private String level;
    private Boolean fallback;
    private AiQuotaStatusDTO quota;
    @JsonAlias("status_message")
    private String statusMessage;
    @JsonAlias("error_code")
    private String errorCode;
    @JsonAlias("http_status")
    private Integer httpStatus;
    private String provider;
    private String reason;
    private Boolean retryable;
}