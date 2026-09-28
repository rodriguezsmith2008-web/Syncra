package com.syncra.gestion_proyectos.dto.document;

import lombok.Data;

/**
 * Versión liviana de un documento: solo lo necesario para sincronizar el
 * título/ícono de tarjetas y filas que enlazan a otros documentos, sin
 * transferir el contenido completo (que puede ser muy pesado).
 */
@Data
public class DocumentSummaryResponseDTO {

    private Long id;
    private String title;
    private String coverImageUrl;
}
