package com.nutrivida.admin;

import com.nutrivida.admin.config.SecurityConfig;
import com.nutrivida.admin.kafka.KafkaAdminController;
import com.nutrivida.admin.kafka.KafkaAdminService;
import com.nutrivida.admin.rabbitmq.RabbitAdminController;
import com.nutrivida.admin.rabbitmq.RabbitAdminService;
import com.nutrivida.admin.rabbitmq.dto.RabbitDtos.QueueInfoResponse;
import com.nutrivida.admin.web.ResourceNotFoundException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RabbitAdminController.class, KafkaAdminController.class})
@Import(SecurityConfig.class)
class AdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitAdminService rabbitAdminService;

    @MockBean
    private KafkaAdminService kafkaAdminService;

    private static RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_Administrador"));
    }

    @Test
    void exigeTokenYRolAdministrador() throws Exception {
        mockMvc.perform(get("/api/admin/kafka/topics")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/kafka/topics")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Secretaria"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/kafka/topics").with(admin())).andExpect(status().isOk());
    }

    @Test
    void creaColaValidada() throws Exception {
        when(rabbitAdminService.createQueue(any())).thenReturn(new QueueInfoResponse("pagos.queue", 0, 0));

        mockMvc.perform(post("/api/admin/rabbitmq/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"pagos.queue\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("pagos.queue"));

        mockMvc.perform(post("/api/admin/rabbitmq/queues").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"cola inválida!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void eliminarColaInexistenteDevuelve404() throws Exception {
        doThrow(new ResourceNotFoundException("La cola 'x' no existe")).when(rabbitAdminService).deleteQueue("x");

        mockMvc.perform(delete("/api/admin/rabbitmq/queues/x").with(admin())).andExpect(status().isNotFound());
    }

    @Test
    void validaTopicoYMapeaConflicto() throws Exception {
        mockMvc.perform(post("/api/admin/kafka/topics").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"t1\",\"partitions\":3,\"replicationFactor\":2,\"cleanupPolicy\":\"borrar\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cleanupPolicy").exists());

        when(kafkaAdminService.createTopic(any())).thenThrow(new TopicExistsException("Topic 't1' already exists."));
        mockMvc.perform(post("/api/admin/kafka/topics").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"t1\",\"partitions\":3,\"replicationFactor\":2,\"retentionMs\":86400000,\"cleanupPolicy\":\"compact,delete\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rechazaParametroDeRutaInvalido() throws Exception {
        mockMvc.perform(get("/api/admin/kafka/topics/nombre con espacios").with(admin()))
                .andExpect(status().isBadRequest());
    }
}
