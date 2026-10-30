package com.nutrivida.admin.rabbitmq;

import com.nutrivida.admin.rabbitmq.dto.RabbitDtos.BindingRequest;
import com.nutrivida.admin.rabbitmq.dto.RabbitDtos.CreateExchangeRequest;
import com.nutrivida.admin.rabbitmq.dto.RabbitDtos.CreateQueueRequest;
import com.nutrivida.admin.rabbitmq.dto.RabbitDtos.QueueInfoResponse;
import com.nutrivida.admin.web.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.stereotype.Service;

@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final AmqpAdmin amqpAdmin;

    public RabbitAdminService(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    public QueueInfoResponse createQueue(CreateQueueRequest request) {
        QueueBuilder builder = Boolean.FALSE.equals(request.durable())
                ? QueueBuilder.nonDurable(request.name())
                : QueueBuilder.durable(request.name());
        if ("quorum".equals(request.type())) {
            builder.quorum();
        }
        if (request.deadLetterExchange() != null) {
            builder.deadLetterExchange(request.deadLetterExchange());
        }
        if (request.deadLetterRoutingKey() != null) {
            builder.deadLetterRoutingKey(request.deadLetterRoutingKey());
        }
        if (request.messageTtlMs() != null) {
            builder.ttl(request.messageTtlMs());
        }
        Queue queue = builder.build();
        amqpAdmin.declareQueue(queue);
        log.info("[Admin RabbitMQ] Cola creada: {} {}", queue.getName(), queue.getArguments());
        return getQueue(queue.getName());
    }

    public QueueInfoResponse getQueue(String name) {
        QueueInformation info = amqpAdmin.getQueueInfo(name);
        if (info == null) {
            throw new ResourceNotFoundException("La cola '" + name + "' no existe");
        }
        return new QueueInfoResponse(info.getName(), info.getMessageCount(), info.getConsumerCount());
    }

    public void deleteQueue(String name) {
        getQueue(name);
        amqpAdmin.deleteQueue(name);
        log.info("[Admin RabbitMQ] Cola eliminada: {}", name);
    }

    public void createExchange(CreateExchangeRequest request) {
        Exchange exchange = new ExchangeBuilder(request.name(), request.type())
                .durable(!Boolean.FALSE.equals(request.durable()))
                .build();
        amqpAdmin.declareExchange(exchange);
        log.info("[Admin RabbitMQ] Exchange creado: {} ({})", request.name(), request.type());
    }

    public void deleteExchange(String name) {
        amqpAdmin.deleteExchange(name);
        log.info("[Admin RabbitMQ] Exchange eliminado: {}", name);
    }

    public void createBinding(BindingRequest request) {
        amqpAdmin.declareBinding(toBinding(request));
        log.info("[Admin RabbitMQ] Binding creado: {} -> {} ({})", request.exchange(), request.queue(), routingKey(request));
    }

    public void deleteBinding(BindingRequest request) {
        amqpAdmin.removeBinding(toBinding(request));
        log.info("[Admin RabbitMQ] Binding eliminado: {} -> {} ({})", request.exchange(), request.queue(), routingKey(request));
    }

    private static Binding toBinding(BindingRequest request) {
        return new Binding(request.queue(), Binding.DestinationType.QUEUE, request.exchange(), routingKey(request), null);
    }

    private static String routingKey(BindingRequest request) {
        return request.routingKey() == null ? "" : request.routingKey();
    }
}
