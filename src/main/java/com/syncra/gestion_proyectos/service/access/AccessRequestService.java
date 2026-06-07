package com.syncra.gestion_proyectos.service.access;

import java.util.List;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.Access.AccessMessageDTO;
import com.syncra.gestion_proyectos.dto.Access.AccessRequestDTO;
import com.syncra.gestion_proyectos.dto.Access.AccessResponseDTO;
import com.syncra.gestion_proyectos.entity.access.AccessRequestEntity;
import com.syncra.gestion_proyectos.entity.user.UsersEntity;
import com.syncra.gestion_proyectos.enums.AccessStatusEnum;
import com.syncra.gestion_proyectos.enums.UserStatusEnum;
import com.syncra.gestion_proyectos.repository.access.AcessRequestRepository;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccessRequestService {

    private final AcessRequestRepository accessRequestRepository;
    private final UsersRepository usersRepository;

    //Crea la peticion de acceso y se valida si ya hay una solicitud pendiente con ese correoo o si el correo ya esta registardo 

    public AccessMessageDTO<String> createRequest(AccessRequestDTO request) {

        AccessMessageDTO<String> message = new AccessMessageDTO<>();

        boolean existeRequest = accessRequestRepository.existsByEmailAndStatus(
            request.getEmail(), AccessStatusEnum.PENDING
        );
        if (existeRequest) {
            message.setMessage("Ya existe una solicitud pendiente con este email");
            return message;
        }

        boolean existeUser = usersRepository.existsByEmail(request.getEmail());
        if (existeUser) {
            message.setMessage("Este email ya está registrado en el sistema");
            return message;
        }

//se guarda la con el estado pendoiente
        AccessRequestEntity entity = new AccessRequestEntity();
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setEmail(request.getEmail());
        entity.setDocumentNumber(request.getDocumentNumber());
        entity.setGroupName(request.getGroupName());
        entity.setRole(request.getRole());

        accessRequestRepository.save(entity);
        message.setMessage("Solicitud enviada correctamente");
        return message;
    }


    //trae las solicitudes pendientes, y las convierte en la respuesta
    public AccessMessageDTO<List<AccessResponseDTO>> findAllPending() {

        AccessMessageDTO<List<AccessResponseDTO>> message = new AccessMessageDTO<>();

        List<AccessRequestEntity> solicitudes = accessRequestRepository
            .findAllByStatus(AccessStatusEnum.PENDING);

        if (solicitudes.isEmpty()) {
            message.setMessage("No hay solicitudes pendientes");
            return message;
        }

        List<AccessResponseDTO> response = solicitudes.stream().map(s -> {
            AccessResponseDTO dto = new AccessResponseDTO();
            dto.setId(s.getId());
            dto.setFirstName(s.getFirstName());
            dto.setLastName(s.getLastName());
            dto.setEmail(s.getEmail());
            dto.setDocumentNumber(s.getDocumentNumber());
            dto.setGroupName(s.getGroupName());
            dto.setStatus(s.getStatus().name());
            dto.setCreatedAt(s.getCreatedAt().toString());
            return dto;
        }).toList();

        message.setMessage("Solicitudes encontradas");
        message.setData(response);
        return message;
    }


    //Valida las solicitudes, si ya existe la solicitud, cambia el estado, si ya se uso ese correo, envia la informacion a usuarios 
    
    public AccessMessageDTO<String> approveRequest(Long id) {

        AccessMessageDTO<String> message = new AccessMessageDTO<>();

        var requestO = accessRequestRepository.findById(id);
        if (requestO.isEmpty()) {
            message.setMessage("Solicitud no encontrada");
            return message;
        }

        AccessRequestEntity request = requestO.get();

        if (!request.getStatus().equals(AccessStatusEnum.PENDING)) {
            message.setMessage("La solicitud ya fue procesada");
            return message;
        }

        boolean existeUser = usersRepository.existsByEmail(request.getEmail());
        if (existeUser) {
            message.setMessage("El email ya está registrado como usuario");
            return message;
        }

        UsersEntity newUser = new UsersEntity();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setDocumentNumber(request.getDocumentNumber());
        newUser.setGroupName(request.getGroupName());
        newUser.setRole(request.getRole());
        newUser.setPassword("Syncra_" + request.getDocumentNumber());
        newUser.setStatus(UserStatusEnum.IN_TRAINING);

        usersRepository.save(newUser);

        request.setStatus(AccessStatusEnum.APPROVED);
        accessRequestRepository.save(request);

        message.setMessage("Usuario creado exitosamente");
        return message;
    }


    //Este es por si el admin rechasa la peticion yu valida, si ya existe la solicitud, si ya fue aprovada y si fue rechazada, cambia el estado
   
    public AccessMessageDTO<String> rejectRequest(Long id) {

        AccessMessageDTO<String> message = new AccessMessageDTO<>();

        var requestO = accessRequestRepository.findById(id);
        if (requestO.isEmpty()) {
            message.setMessage("Solicitud no encontrada");
            return message;
        }

        AccessRequestEntity request = requestO.get();

        if (!request.getStatus().equals(AccessStatusEnum.PENDING)) {
            message.setMessage("La solicitud ya fue procesada");
            return message;
        }

        request.setStatus(AccessStatusEnum.REJECTED);
        accessRequestRepository.save(request);

        message.setMessage("Solicitud de " + request.getFirstName() + " " + request.getLastName() + " rechazada");
        return message;
    }
}