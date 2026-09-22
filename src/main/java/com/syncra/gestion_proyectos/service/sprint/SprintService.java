package com.syncra.gestion_proyectos.service.sprint;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.sprint.SprintRequestDTO;
import com.syncra.gestion_proyectos.dto.sprint.SprintResponseDTO;
import com.syncra.gestion_proyectos.entity.sprint.SprintEntity;
import com.syncra.gestion_proyectos.enums.SprintStatusEnum;
import com.syncra.gestion_proyectos.repository.sprint.SprintRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final SprintRealtimeService sprintRealtimeService;

    public SprintResponseDTO create(SprintRequestDTO request) {

        validateDates(request.getStartDate(), request.getEndDate());

        SprintEntity sprintEntity = new SprintEntity();
        sprintEntity.setProjectId(request.getProjectId());
        sprintEntity.setName(request.getName());
        sprintEntity.setStartDate(request.getStartDate());
        sprintEntity.setEndDate(request.getEndDate());
        sprintEntity.setStatus(SprintStatusEnum.PLANNED);

        SprintEntity saved = sprintRepository.save(sprintEntity);
        sprintRealtimeService.publishChanged(saved.getProjectId(), saved.getId(), "CREATED");
        return toResponseDTO(saved);

    }

    public List<SprintResponseDTO> getByProject(Long projectId) {
        return sprintRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public SprintResponseDTO getById(Long id) {
        return toResponseDTO(findEntityOrThrow(id));
    }

    public SprintResponseDTO update(Long id, SprintRequestDTO dto) {
        SprintEntity entity = findEntityOrThrow(id);

        if (dto.getProjectId() != null) {
            entity.setProjectId(dto.getProjectId());
        }
        if (dto.getName() != null) {
            entity.setName(dto.getName());
        }
        if (dto.getStartDate() != null) {
            entity.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            entity.setEndDate(dto.getEndDate());
        }

        validateDates(entity.getStartDate(), entity.getEndDate());

        SprintEntity saved = sprintRepository.save(entity);
        sprintRealtimeService.publishChanged(saved.getProjectId(), saved.getId(), "UPDATED");
        return toResponseDTO(saved);
    }

    public void delete(Long id) {
        SprintEntity entity = findEntityOrThrow(id);
        sprintRepository.delete(entity);
        sprintRealtimeService.publishChanged(entity.getProjectId(), entity.getId(), "DELETED");
    }

    private SprintEntity findEntityOrThrow(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sprint no encontrado con id: " + id));
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la fecha de inicio");
        }
    }

    private SprintResponseDTO toResponseDTO(SprintEntity entity) {
        SprintResponseDTO dto = new SprintResponseDTO();
        dto.setId(entity.getId());
        dto.setProjectId(entity.getProjectId());
        dto.setName(entity.getName());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

}
