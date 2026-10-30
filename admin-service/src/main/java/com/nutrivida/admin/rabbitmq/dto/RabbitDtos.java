package com.nutrivida.admin.rabbitmq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * DTOs validados de la API de administración de RabbitMQ.
 */
public final class RabbitDtos {

    /** Nombres válidos: letras, números, '.', '_', ':' y '-'. Los prefijos "amq." están reservados por el broker. */
    public static final String NAME_REGEX = "^(?!amq\\.)[a-zA-Z0-9_.:-]{1,255}$";
    public static final String NAME_MESSAGE = "Nombre inválido (solo letras, números, '.', '_', ':' o '-'; sin prefijo 'amq.')";

    private RabbitDtos() {
    }

    public record CreateQueueRequest(
            @NotBlank @Pattern(regexp = NAME_REGEX, message = NAME_MESSAGE) String name,
            Boolean durable,
            @Pattern(regexp = "classic|quorum", message = "type debe ser 'classic' o 'quorum'") String type,
            @Pattern(regexp = NAME_REGEX, message = NAME_MESSAGE) String deadLetterExchange,
            @Pattern(regexp = "^[a-zA-Z0-9_.*#-]{0,255}$", message = "Routing key inválida") String deadLetterRoutingKey,
            @Positive Integer messageTtlMs) {
    }

    public record CreateExchangeRequest(
            @NotBlank @Pattern(regexp = NAME_REGEX, message = NAME_MESSAGE) String name,
            @NotBlank @Pattern(regexp = "direct|topic|fanout|headers",
                    message = "type debe ser direct, topic, fanout o headers") String type,
            Boolean durable) {
    }

    public record BindingRequest(
            @NotBlank @Pattern(regexp = NAME_REGEX, message = NAME_MESSAGE) String exchange,
            @NotBlank @Pattern(regexp = NAME_REGEX, message = NAME_MESSAGE) String queue,
            @Pattern(regexp = "^[a-zA-Z0-9_.*#-]{0,255}$", message = "Routing key inválida") String routingKey) {
    }

    public record QueueInfoResponse(String name, int messageCount, int consumerCount) {
    }
}
