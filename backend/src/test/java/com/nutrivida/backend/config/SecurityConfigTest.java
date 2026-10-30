package com.nutrivida.backend.config;

import com.nutrivida.backend.controller.ClinicaController;
import com.nutrivida.backend.security.SecurityProperties;
import com.nutrivida.backend.service.ClinicaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClinicaController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityProperties securityProperties;

    @MockBean
    private ClinicaService clinicaService;

    @Test
    void rechazaPeticionesSinToken() throws Exception {
        mockMvc.perform(get("/api/reservas"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rechazaRolSinPermiso() throws Exception {
        mockMvc.perform(get("/api/reservas").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Paciente"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void permiteRolAutorizado() throws Exception {
        mockMvc.perform(get("/api/reservas").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Secretaria"))))
                .andExpect(status().isOk());
    }

    @Test
    void soloAdministradorCreaNutricionistas() throws Exception {
        String body = "{\"nombre\":\"Dra. Navarro\",\"especialidad\":\"Clínica\",\"email\":\"n@nutrivida.cl\"}";
        mockMvc.perform(post("/api/nutricionistas").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Secretaria"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/nutricionistas").contentType(MediaType.APPLICATION_JSON).content(body)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Administrador"))))
                .andExpect(status().isOk());
    }

    @Test
    void permitePreflightCorsDeOrigenConfigurado() throws Exception {
        mockMvc.perform(options("/api/reservas")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    @Test
    void validaIssuerAudienceYVigencia() {
        var validator = SecurityConfig.tokenValidator(securityProperties);
        String issuer = securityProperties.issuers().get(0);
        String audience = securityProperties.audiences().get(0);

        assertThat(validator.validate(token(issuer, audience, Instant.now().plusSeconds(600))).hasErrors()).isFalse();
        assertThat(validator.validate(token("https://emisor-falso", audience, Instant.now().plusSeconds(600))).hasErrors()).isTrue();
        assertThat(validator.validate(token(issuer, "api://otra-api", Instant.now().plusSeconds(600))).hasErrors()).isTrue();
        assertThat(validator.validate(token(issuer, audience, Instant.now().minusSeconds(3600))).hasErrors()).isTrue();
    }

    private static Jwt token(String issuer, String audience, Instant expiresAt) {
        return new Jwt("token", expiresAt.minusSeconds(3600), expiresAt,
                Map.of("alg", "RS256"),
                Map.of("iss", issuer, "aud", List.of(audience), "sub", "usuario"));
    }
}
