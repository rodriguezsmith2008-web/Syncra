package com.syncra.gestion_proyectos.controller.contact;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.syncra.gestion_proyectos.dto.contact.ContactRequestDto;
import com.syncra.gestion_proyectos.service.email.ContactService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<String> sendMessage(@Valid @RequestBody ContactRequestDto dto) {
        contactService.sendContactMessage(dto);
        return ResponseEntity.ok("mensaje enviado correctamente");
    }
}