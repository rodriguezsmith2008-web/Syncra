package com.syncra.gestion_proyectos.dto.ai;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class AiServiceRequestDTO {
    private String message;
    private AiProjectContextDTO context;
    private List<AiHistoryItemDTO> history;
    private String model;
    private List<Map<String, Object>> tools;
}