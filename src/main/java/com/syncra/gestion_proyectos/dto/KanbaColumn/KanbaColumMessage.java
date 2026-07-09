package com.syncra.gestion_proyectos.dto.KanbaColumn;

import lombok.Data;

@Data
public class KanbaColumMessage<T> {
    
    private String message;
    private T data;
}
