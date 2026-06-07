package com.syncra.gestion_proyectos.dto.Access;

import lombok.Data;

@Data

public class AccessMessageDTO<T> {
    private String message;
    private T data;
}
