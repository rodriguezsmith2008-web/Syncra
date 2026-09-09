package com.syncra.gestion_proyectos.dto.ai;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
public class AiChatResponseDTO {
    private String reply;
    @JsonAlias("suggested_card")
    private AiSuggestedCardDTO suggestedCard;
    private List<AiActionDTO> actions;
}