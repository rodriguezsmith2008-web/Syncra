package com.syncra.gestion_proyectos.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

@Data
public class AiToolCallDTO {
    private String id;
    private String type;
    @JsonAlias("function")
    private AiFunctionCallDTO function;
}
