package com.nutrivida.backend.controller;

import com.nutrivida.backend.dto.CreateQueueRequest;
import com.nutrivida.backend.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/queues")
public class RabbitAdminController {

    private final RabbitAdminService adminService;

    public RabbitAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<String> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        adminService.createQueue(request.getQueueName());
        return ResponseEntity.ok("Cola '" + request.getQueueName() + "' creada exitosamente.");
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<String> deleteQueue(@PathVariable String name) {
        boolean deleted = adminService.deleteQueue(name);
        if (deleted) {
            return ResponseEntity.ok("Cola '" + name + "' eliminada exitosamente.");
        } else {
            return ResponseEntity.badRequest().body("No se pudo eliminar la cola '" + name + "'. Puede que no exista.");
        }
    }
}