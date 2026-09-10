package com.syncra.gestion_proyectos.dto.ai;

import lombok.Data;

@Data
public class AiQuotaStatusDTO {
    private Integer requestsUsed;
    private Integer requestsLimit;
    private Integer requestsRemaining;
    private Long tokensUsed;
    private Long tokensLimit;
    private Long tokensRemaining;
}
