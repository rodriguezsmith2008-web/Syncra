package com.syncra.gestion_proyectos.controller.access;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.access.AccessMessageDTO;
import com.syncra.gestion_proyectos.dto.access.AccessRequestDTO;
import com.syncra.gestion_proyectos.dto.access.AccessResponseDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.access.AccessRequestService;

import lombok.RequiredArgsConstructor;

//Le dice a spring que peticion http retornar JSON
@RestController

// ruta de acceso
@RequestMapping("/access-requests")

// lombok, inyecta al servicio sin usar constructor
@RequiredArgsConstructor
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    @PostMapping
    public ResponseEntity<AccessMessageDTO<String>> createRequest(
            // toma el json y lo envia al RequestDTO
            @RequestBody AccessRequestDTO request) {
        AccessMessageDTO<String> response = accessRequestService.createRequest(request);
        String message = response.getMessage();

        if ("Solicitud enviada correctamente".equals(message)) {
            return ResponseEntity.ok(response);
        }

        if ("Correo inválido.".equals(message) || "Documento inválido.".equals(message)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        return ResponseEntity.badRequest().body(response);
    }

    // Trae las solicitudes pendientes
    // solo el administrador las acepta
    @RequireRole(RoleUserEnum.ADMIN)
    @GetMapping
    public ResponseEntity<AccessMessageDTO<List<AccessResponseDTO>>> findAllPending() {
        return ResponseEntity.ok(accessRequestService.findAllPending());
    }

    // Aprueba la solicitd de registro y cambia el estado (con pach ya que no vamos
    // a modificar nada mas que el estado)
    // solo el administrador cambia el estado
    @RequireRole(RoleUserEnum.ADMIN)
    @PatchMapping("/{id}/approve")
    public ResponseEntity<AccessMessageDTO<String>> approve(
            @PathVariable Long id) {
        return ResponseEntity.ok(accessRequestService.approveRequest(id));
    }

    // rechazar solicitud de registro
    // solo el administrador rechaza la solicitud
    @RequireRole(RoleUserEnum.ADMIN)
    @PatchMapping("/{id}/reject")
    public ResponseEntity<AccessMessageDTO<String>> reject(
            @PathVariable Long id) {
        return ResponseEntity.ok(accessRequestService.rejectRequest(id));
    }
}