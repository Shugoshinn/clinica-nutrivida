package com.nutrivida.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio administrador: gestiona colas, exchanges y bindings de RabbitMQ
 * y tópicos, configuraciones y consumer groups de Kafka.
 */
@SpringBootApplication
public class AdminServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
