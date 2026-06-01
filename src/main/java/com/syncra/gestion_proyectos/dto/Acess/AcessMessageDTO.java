package com.syncra.gestion_proyectos.dto.Acess;

import lombok.Data;

@Data

public class AcessMessageDTO<T> {
    private String message;
    private T data;
}
