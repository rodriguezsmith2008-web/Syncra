package com.syncra.gestion_proyectos.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiMessageRequestDTO {
    @NotBlank(message = "El mensaje no puede estar vacío")
    @Size(max = 4000, message = "El mensaje no puede superar 4000 caracteres")
    private String message;
}