package com.nutrivida.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class CreateQueueRequest {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "El nombre de la cola contiene caracteres inválidos")
    private String queueName;

    // Constructores
    public CreateQueueRequest() {}

    public CreateQueueRequest(String queueName) {
        this.queueName = queueName;
    }

    // Getters y Setters
    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}