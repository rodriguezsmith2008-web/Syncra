package com.syncra.gestion_proyectos.dto.access;

import lombok.Data;

@Data

public class AccessMessageDTO<T> {
    private String message;
    private T data;
}
