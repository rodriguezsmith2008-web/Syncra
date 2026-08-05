package com.syncra.gestion_proyectos.controller.publicstats;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.publicstats.PublicStatsDTO;
import com.syncra.gestion_proyectos.service.publicstats.PublicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

    @GetMapping("/stats")
    public ResponseEntity<PublicStatsDTO> getStats() {
        return ResponseEntity.ok(publicService.getStats());
    }

}