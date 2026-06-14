package com.syncra.gestion_proyectos.dto.users;

import lombok.Data;

@Data
public class HttpGlobalResponse<T> {

    private T data;
    private String message;

}
