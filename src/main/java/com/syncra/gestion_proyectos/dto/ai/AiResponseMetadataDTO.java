package com.syncra.gestion_proyectos.dto.ai;

import lombok.Data;

@Data
public class AiResponseMetadataDTO {
    private String model;
    private String level;
    private Boolean fallback;
    private AiQuotaStatusDTO quota;
    private String statusMessage;
}
