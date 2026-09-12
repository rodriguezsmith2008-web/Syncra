package com.syncra.gestion_proyectos.dto.realtime;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentSyncCatchUp {

    private Long documentId;
    private List<DocumentSyncMessage> updates;
    private boolean puedeSembrar;
}
