package com.syncra.gestion_proyectos.dto.ai;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

@Data
public class AiFunctionCallDTO {
    private String name;
    @JsonAlias("arguments")
    private Map<String, Object> arguments;
}
