package com.syncra.gestion_proyectos.dto.realtime;

import lombok.Data;

@Data
public class DocumentSyncMessage {

    private String type;
    private String update;
    private String awareness;
    private String senderSessionId;
}
