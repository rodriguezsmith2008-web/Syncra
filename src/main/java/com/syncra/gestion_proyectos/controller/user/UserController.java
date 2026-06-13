package com.syncra.gestion_proyectos.controller.user;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.syncra.gestion_proyectos.dto.users.UserMessage;
import com.syncra.gestion_proyectos.dto.users.UserRequestDTO;
import com.syncra.gestion_proyectos.dto.users.UserResponseDTO;
import com.syncra.gestion_proyectos.dto.users.UserUpdateDTO;
import com.syncra.gestion_proyectos.enums.RoleUserEnum;
import com.syncra.gestion_proyectos.security.RequireRole;
import com.syncra.gestion_proyectos.service.user.UserService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    /**
     * Servicio de usuarios
     */
    private final UserService userService;

    @RequireRole(RoleUserEnum.ADMIN)
    @GetMapping("/list-users")
    public ResponseEntity<List<UserResponseDTO>> listUsers() {
        try {
            List<UserResponseDTO> response = userService.listUsers();
            return ResponseEntity.status(HttpStatus.FOUND).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @RequireRole(RoleUserEnum.ADMIN)
    @GetMapping("/get-user/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        try {
            UserResponseDTO response = userService.getUserById(id);
            return ResponseEntity.status(HttpStatus.FOUND).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @RequireRole(RoleUserEnum.ADMIN)
    @PostMapping("/create-user")
    public ResponseEntity<UserMessage> createUser(@RequestBody UserRequestDTO request) {
        try {
            UserMessage response = userService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @RequireRole(RoleUserEnum.ADMIN)
    @PutMapping("/update-user/{id}")
    public ResponseEntity<UserMessage> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateDTO request) {

        try {
            UserMessage response = userService.updateUser(id, request);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @RequireRole(RoleUserEnum.ADMIN)
    @DeleteMapping("/delete-user/{id}")
    public ResponseEntity<UserMessage> deleteUser(@PathVariable Long id) {
        try {
            UserMessage response = userService.deleteUser(id);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}