package com.nutrivida.backend.consumer; 

import com.nutrivida.backend.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ReservaConsumer {

    @RabbitListener(queues = RabbitMQConfig.RESERVA_QUEUE)
    public void recibirNotificacionReserva(String mensaje, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        try {
            System.out.println("[✔] Mensaje recibido en el consumidor del backend: " + mensaje);
            
            // Aquí iría tu lógica de negocio (ej. guardar en BD, enviar correo)
            
            // Confirmación explícita de éxito (ACK)
            channel.basicAck(tag, false);
            
        } catch (Exception e) {
            System.err.println("[X] Error procesando el mensaje: " + e.getMessage());
            
            // Rechazo explícito de fallo (NACK). 
            // El último parámetro en 'false' evita que el mensaje defectuoso se vuelva a encolar infinitamente.
            channel.basicNack(tag, false, false); 
        }
    }
}