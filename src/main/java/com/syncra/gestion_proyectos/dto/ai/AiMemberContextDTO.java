package com.syncra.gestion_proyectos.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiMemberContextDTO {
    private Long userId;
    private String name;
    private String role;
}