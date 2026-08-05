package com.syncra.gestion_proyectos.service.publicstats;

import org.springframework.stereotype.Service;

import com.syncra.gestion_proyectos.dto.publicstats.PublicStatsDTO;
import com.syncra.gestion_proyectos.repository.user.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PublicService {

    private final UsersRepository usersRepository;

    public PublicStatsDTO getStats() {

        return new PublicStatsDTO(
                usersRepository.count()
        );

    }

}