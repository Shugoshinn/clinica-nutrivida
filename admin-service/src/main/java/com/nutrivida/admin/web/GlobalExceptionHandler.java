package com.nutrivida.admin.web;

import jakarta.validation.ConstraintViolationException;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidReplicationFactorException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Traduce errores de validación, RabbitMQ y Kafka a códigos HTTP coherentes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- 400: validación de entrada ---
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errores.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return body(HttpStatus.BAD_REQUEST, "Datos inválidos", errores);
    }

    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<Map<String, Object>> handlePathValidation(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, "Parámetro de ruta inválido", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return body(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no tiene un formato válido", Map.of());
    }

    @ExceptionHandler({InvalidPartitionsException.class, InvalidReplicationFactorException.class,
            InvalidConfigurationException.class, InvalidRequestException.class, InvalidTopicException.class,
            PolicyViolationException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    // --- 404 / 409 ---
    @ExceptionHandler({ResourceNotFoundException.class, UnknownTopicOrPartitionException.class,
            GroupIdNotFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(Exception ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler({TopicExistsException.class, GroupNotEmptyException.class})
    public ResponseEntity<Map<String, Object>> handleConflict(Exception ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    // --- Errores de los brokers ---
    @ExceptionHandler({ServiceUnavailableException.class, AmqpConnectException.class,
            org.apache.kafka.common.errors.TimeoutException.class})
    public ResponseEntity<Map<String, Object>> handleUnavailable(Exception ex) {
        log.error("Broker no disponible: {}", ex.getMessage());
        return body(HttpStatus.SERVICE_UNAVAILABLE, "El broker de mensajería no está disponible", Map.of());
    }

    // Ej. PRECONDITION_FAILED al redeclarar una cola con otros argumentos, o NOT_FOUND de un exchange inexistente
    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<Map<String, Object>> handleAmqp(AmqpException ex) {
        String detalle = rootMessage(ex);
        log.warn("RabbitMQ rechazó la operación: {}", detalle);
        return body(HttpStatus.CONFLICT, "RabbitMQ rechazó la operación: " + detalle, Map.of());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleKafka(ApiException ex) {
        log.warn("Kafka rechazó la operación: {}", ex.getMessage());
        return body(HttpStatus.BAD_GATEWAY, "Kafka rechazó la operación: " + ex.getMessage(), Map.of());
    }

    private static String rootMessage(Throwable ex) {
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? ex.getMessage() : root.getMessage();
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, Map<String, String> errores) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (!errores.isEmpty()) {
            body.put("errors", errores);
        }
        return ResponseEntity.status(status).body(body);
    }
}
