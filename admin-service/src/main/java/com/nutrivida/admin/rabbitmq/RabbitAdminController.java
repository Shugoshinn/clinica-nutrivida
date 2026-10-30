package com.nutrivida.admin.rabbitmq;

import com.nutrivida.admin.rabbitmq.dto.RabbitDtos;
import com.nutrivida.admin.rabbitmq.dto.RabbitDtos.BindingRequest;
import com.nutrivida.admin.rabbitmq.dto.RabbitDtos.CreateExchangeRequest;
import com.nutrivida.admin.rabbitmq.dto.RabbitDtos.CreateQueueRequest;
import com.nutrivida.admin.rabbitmq.dto.RabbitDtos.QueueInfoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/rabbitmq")
@Validated
public class RabbitAdminController {

    private final RabbitAdminService adminService;

    public RabbitAdminController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    // --- Colas ---
    @PostMapping("/queues")
    public ResponseEntity<QueueInfoResponse> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createQueue(request));
    }

    @GetMapping("/queues/{name}")
    public QueueInfoResponse getQueue(@PathVariable @Pattern(regexp = RabbitDtos.NAME_REGEX) String name) {
        return adminService.getQueue(name);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable @Pattern(regexp = RabbitDtos.NAME_REGEX) String name) {
        adminService.deleteQueue(name);
        return ResponseEntity.noContent().build();
    }

    // --- Exchanges ---
    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> createExchange(@Valid @RequestBody CreateExchangeRequest request) {
        adminService.createExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("name", request.name(), "type", request.type()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable @Pattern(regexp = RabbitDtos.NAME_REGEX) String name) {
        adminService.deleteExchange(name);
        return ResponseEntity.noContent().build();
    }

    // --- Bindings ---
    @PostMapping("/bindings")
    public ResponseEntity<BindingRequest> createBinding(@Valid @RequestBody BindingRequest request) {
        adminService.createBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(@Valid @RequestBody BindingRequest request) {
        adminService.deleteBinding(request);
        return ResponseEntity.noContent().build();
    }
}
