package com.syncra.gestion_proyectos.dto.ai;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class AiChatResponseDTO {
    private Boolean success = true;
    private String reply;
    @JsonAlias("suggested_card")
    private AiSuggestedCardDTO suggestedCard;
    private List<AiActionDTO> actions;
    @JsonAlias("tool_calls")
    private List<AiToolCallDTO> toolCalls;
    private String model;
    private String level;
    private Boolean fallback;
    @JsonAlias("quota")
    private AiQuotaStatusDTO quota;
    @JsonAlias("usage")
    private java.util.Map<String, Object> usage;
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