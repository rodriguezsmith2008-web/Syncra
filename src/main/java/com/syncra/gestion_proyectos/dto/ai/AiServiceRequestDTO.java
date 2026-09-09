package com.syncra.gestion_proyectos.dto.ai;

import java.util.List;
import lombok.Data;

@Data
public class AiServiceRequestDTO {
    private String message;
    private AiProjectContextDTO context;
    private List<AiHistoryItemDTO> history;
}